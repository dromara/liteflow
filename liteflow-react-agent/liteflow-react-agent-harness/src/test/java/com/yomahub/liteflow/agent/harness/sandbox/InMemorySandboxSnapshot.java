package com.yomahub.liteflow.agent.harness.sandbox;

import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshot;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/** Test-only snapshot store that exercises the real AgentScope snapshot contracts. */
public final class InMemorySandboxSnapshot implements SandboxSnapshotSpec, SandboxSnapshot {

    private final Map<String, byte[]> archives;
    private final List<String> events;
    private final String id;

    public InMemorySandboxSnapshot(List<String> events) {
        this(new ConcurrentHashMap<>(), events, null);
    }

    private InMemorySandboxSnapshot(
            Map<String, byte[]> archives, List<String> events, String id) {
        this.archives = Objects.requireNonNull(archives, "archives");
        this.events = Objects.requireNonNull(events, "events");
        this.id = id;
    }

    @Override
    public SandboxSnapshot build(String snapshotId) {
        return new InMemorySandboxSnapshot(archives, events, snapshotId);
    }

    @Override
    public void persist(InputStream workspaceArchive) throws Exception {
        requireBuilt();
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        workspaceArchive.transferTo(bytes);
        archives.put(id, bytes.toByteArray());
        events.add("snapshot-persist:" + id);
    }

    @Override
    public InputStream restore() {
        requireBuilt();
        byte[] archive = archives.get(id);
        if (archive == null) {
            throw new IllegalStateException("snapshot is not restorable: " + id);
        }
        events.add("snapshot-restore:" + id);
        return new ByteArrayInputStream(archive);
    }

    @Override
    public boolean isRestorable() {
        requireBuilt();
        return archives.containsKey(id);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
        return "memory";
    }

    public int snapshotCount() {
        return archives.size();
    }

    private void requireBuilt() {
        if (id == null) {
            throw new IllegalStateException("snapshot spec must be built before snapshot use");
        }
    }
}
