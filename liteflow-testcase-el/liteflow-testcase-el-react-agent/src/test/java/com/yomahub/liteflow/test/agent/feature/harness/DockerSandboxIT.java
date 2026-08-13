package com.yomahub.liteflow.test.agent.feature.harness;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.SandboxException;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import io.agentscope.harness.agent.sandbox.snapshot.LocalSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real Docker coverage. This class is compiled by default but is executed only by the explicit
 * {@code agent-docker-it} Failsafe profile, whose preflight requires a reachable daemon and the
 * already-local {@code alpine:3.20} image.
 */
public class DockerSandboxIT {

    private static final String IMAGE = "alpine:3.20";
    private static final String WORKSPACE = "/workspace";
    private static final long MEMORY_BYTES = 64L * 1024 * 1024;
    private static final long CPU_COUNT = 1L;
    private static final List<String> SECURITY_ARGS = List.of(
            "--cap-drop=ALL",
            "--security-opt=no-new-privileges:true",
            "--pids-limit=64");

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path snapshotRoot;

    @Test
    void realDockerSupportsFilesHardTimeoutSnapshotLimitsAndSuccessCleanup() throws Exception {
        DockerSetup setup = dockerSetup();
        Sandbox first = setup.create();
        DockerSandboxState persistedState;
        String firstContainerId;
        try {
            first.start();
            persistedState = (DockerSandboxState) first.getState();
            firstContainerId = persistedState.getContainerId();

            ExecResult write = first.exec(
                    null, "printf '%s' 'snapshot-value' > durable.txt", 5);
            assertTrue(write.ok());
            assertEquals("snapshot-value",
                    first.exec(null, "cat durable.txt", 5).stdout().strip());

            SandboxException.ExecTimeoutException timeout = assertThrows(
                    SandboxException.ExecTimeoutException.class,
                    () -> first.exec(null, "while :; do :; done", 1));
            assertTrue(timeout.getMessage().contains("1"));

            JsonNode hostConfig = inspectHostConfig(firstContainerId);
            assertEquals(MEMORY_BYTES, hostConfig.path("Memory").asLong());
            assertEquals(CPU_COUNT * 1_000_000_000L, hostConfig.path("NanoCpus").asLong());
            assertEquals("none", hostConfig.path("NetworkMode").asText());
        }
        finally {
            first.close();
        }
        assertContainerRemoved(firstContainerId);

        Sandbox restored = setup.client().resume(persistedState);
        String restoredContainerId;
        try {
            restored.start();
            restoredContainerId = ((DockerSandboxState) restored.getState()).getContainerId();
            assertNotEquals(firstContainerId, restoredContainerId);
            assertEquals("snapshot-value",
                    restored.exec(null, "cat durable.txt", 5).stdout().strip());
        }
        finally {
            restored.close();
        }
        assertContainerRemoved(restoredContainerId);
    }

    @Test
    void realDockerRemovesContainerWhenTestedWorkFails() throws Exception {
        Sandbox sandbox = dockerSetup().create();
        AtomicReference<String> containerId = new AtomicReference<>();

        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> {
            try (sandbox) {
                sandbox.start();
                containerId.set(((DockerSandboxState) sandbox.getState()).getContainerId());
                throw new IllegalStateException("intentional Docker IT failure path");
            }
        });

        assertEquals("intentional Docker IT failure path", failure.getMessage());
        assertContainerRemoved(containerId.get());
    }

    private static WorkspaceSpec workspace() {
        WorkspaceSpec workspace = new WorkspaceSpec();
        workspace.setRoot(WORKSPACE);
        workspace.setEntries(new LinkedHashMap<>());
        workspace.setEnvironment(Map.of());
        return workspace;
    }

    private DockerSetup dockerSetup() {
        DockerSandboxClient client = new DockerSandboxClient();
        DockerFilesystemSpec spec = new DockerFilesystemSpec()
                .client(client)
                .image(IMAGE)
                .workspaceRoot(WORKSPACE)
                .memorySizeBytes(MEMORY_BYTES)
                .cpuCount(CPU_COUNT)
                .network("none")
                .additionalRunArgs(SECURITY_ARGS)
                .snapshotSpec(new LocalSnapshotSpec(snapshotRoot))
                .workspaceSpec(workspace());
        spec.workspaceProjectionEnabled(false);
        SandboxContext context = spec.toSandboxContext();
        return new DockerSetup(
                client,
                context.getWorkspaceSpec(),
                context.getSnapshotSpec(),
                (DockerSandboxClientOptions) context.getClientOptions());
    }

    private JsonNode inspectHostConfig(String containerId) throws Exception {
        CommandResult result = docker(
                "inspect", "--format", "{{json .HostConfig}}", containerId);
        assertEquals(0, result.exitCode(), result.stderr());
        return objectMapper.readTree(result.stdout());
    }

    private static void assertContainerRemoved(String containerId) throws Exception {
        CommandResult result = docker("inspect", containerId);
        assertFalse(result.exitCode() == 0,
                () -> "container was not removed: " + containerId + "\n" + result.stdout());
        assertTrue(
                result.stderr().contains("No such object")
                        || result.stderr().contains("No such container"),
                () -> "Docker inspect failed for an unexpected reason: " + result.stderr());
    }

    private static CommandResult docker(String... arguments) throws Exception {
        String[] command = new String[arguments.length + 1];
        command[0] = "docker";
        System.arraycopy(arguments, 0, command, 1, arguments.length);
        Process process = new ProcessBuilder(command).start();
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicReference<Throwable> outputFailure = new AtomicReference<>();
        Thread stdoutReader = drain(
                process.getInputStream(), stdout, "docker-it-stdout", outputFailure);
        Thread stderrReader = drain(
                process.getErrorStream(), stderr, "docker-it-stderr", outputFailure);
        if (!process.waitFor(15, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            if (!process.waitFor(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException(
                        "could not terminate timed-out Docker IT helper: "
                                + String.join(" ", command));
            }
            stdoutReader.join(5_000);
            stderrReader.join(5_000);
            throw new IllegalStateException("Docker IT helper timed out: " + String.join(" ", command));
        }
        stdoutReader.join();
        stderrReader.join();
        if (outputFailure.get() != null) {
            throw new IllegalStateException(
                    "failed to read Docker IT process output", outputFailure.get());
        }
        return new CommandResult(
                process.exitValue(),
                stdout.toString(StandardCharsets.UTF_8),
                stderr.toString(StandardCharsets.UTF_8));
    }

    private static Thread drain(
            InputStream input,
            ByteArrayOutputStream output,
            String name,
            AtomicReference<Throwable> failure) {
        Thread reader = new Thread(() -> {
            try (input; output) {
                input.transferTo(output);
            }
            catch (Exception readFailure) {
                failure.compareAndSet(null, readFailure);
            }
        }, name);
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

    private record CommandResult(int exitCode, String stdout, String stderr) {
    }

    private record DockerSetup(
            DockerSandboxClient client,
            WorkspaceSpec workspace,
            SandboxSnapshotSpec snapshots,
            DockerSandboxClientOptions options) {

        Sandbox create() {
            return client.create(workspace, snapshots, options);
        }
    }
}
