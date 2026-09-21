package com.yomahub.liteflow.agent.harness.sandbox;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.sandbox.AbstractBaseSandbox;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** In-memory sandbox backend with a real {@link AbstractBaseSandbox} lifecycle. */
final class FakeSandbox extends AbstractBaseSandbox {

    private final DockerSandboxState state;
    private final List<String> events;
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();
    private volatile boolean workspaceExists;

    FakeSandbox(DockerSandboxState state, List<String> events) {
        super(state);
        this.state = Objects.requireNonNull(state, "state");
        this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public void start() throws Exception {
        events.add("start:" + state.getSessionId());
        super.start();
    }

    @Override
    public void stop() throws Exception {
        events.add("stop:" + state.getSessionId());
        super.stop();
    }

    @Override
    public void shutdown() {
        events.add("shutdown:" + state.getSessionId());
        doDestroyWorkspace();
    }

    @Override
    protected ExecResult doExec(
            RuntimeContext runtimeContext, String command, int timeoutSeconds) {
        if (runtimeContext == null && command.startsWith("test -d ")) {
            return new ExecResult(workspaceExists ? 0 : 1, "", "", false);
        }
        events.add("exec:" + command);
        if (command.startsWith("write ")) {
            events.add("tool:" + state.getSessionId());
            String[] parts = command.split(" ", 3);
            if (parts.length != 3) {
                return new ExecResult(2, "", "invalid write command", false);
            }
            files.put(parts[1], parts[2].getBytes(StandardCharsets.UTF_8));
            return new ExecResult(0, "written", "", false);
        }
        if (command.startsWith("read ")) {
            events.add("tool:" + state.getSessionId());
            String path = command.substring("read ".length());
            byte[] content = files.get(path);
            return content == null
                    ? new ExecResult(1, "", "not found", false)
                    : new ExecResult(0, new String(content, StandardCharsets.UTF_8), "", false);
        }
        return new ExecResult(0, "ok", "", false);
    }

    @Override
    protected InputStream doPersistWorkspace() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (TarArchiveOutputStream tar = new TarArchiveOutputStream(bytes)) {
            tar.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
            for (Map.Entry<String, byte[]> file : files.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .toList()) {
                TarArchiveEntry entry = new TarArchiveEntry(file.getKey());
                entry.setSize(file.getValue().length);
                tar.putArchiveEntry(entry);
                tar.write(file.getValue());
                tar.closeArchiveEntry();
            }
            tar.finish();
        }
        return new ByteArrayInputStream(bytes.toByteArray());
    }

    @Override
    protected void doHydrateWorkspace(InputStream archive) throws Exception {
        try (TarArchiveInputStream tar = new TarArchiveInputStream(archive)) {
            TarArchiveEntry entry;
            while ((entry = tar.getNextEntry()) != null) {
                if (entry.isFile()) {
                    files.put(entry.getName(), tar.readNBytes(Math.toIntExact(entry.getSize())));
                }
            }
        }
        workspaceExists = true;
    }

    @Override
    protected void doSetupWorkspace() {
        workspaceExists = true;
    }

    @Override
    protected void doDestroyWorkspace() {
        workspaceExists = false;
        files.clear();
    }

    @Override
    protected String getWorkspaceRoot() {
        return state.getWorkspaceSpec().getRoot();
    }

    String file(String path) {
        byte[] content = files.get(path);
        return content == null ? null : new String(content, StandardCharsets.UTF_8);
    }
}
