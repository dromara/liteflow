package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.property.agent.DockerSandboxConfig;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.snapshot.LocalSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

/** Maps LiteFlow Docker settings into a policy-bound AgentScope sandbox spec. */
public final class DockerSandboxConfigurer implements HarnessFilesystemConfigurer {

    private static final List<String> REQUIRED_RUN_ARGS = List.of(
            "--cap-drop=ALL",
            "--security-opt=no-new-privileges:true",
            "--pids-limit=64");

    private final SandboxSnapshotProvider snapshotProvider;
    private final SandboxClient<DockerSandboxClientOptions> sandboxClient;

    public DockerSandboxConfigurer() {
        this(null);
    }

    public DockerSandboxConfigurer(SandboxSnapshotProvider snapshotProvider) {
        this(snapshotProvider, null);
    }

    /**
     * Creates a configurer with a trusted Java sandbox backend.
     *
     * <p>This narrow extension seam is intended for custom backends and deterministic tests. The
     * supplied client still passes through LiteFlow's projection preflight wrapper and cannot
     * replace the policy-bound Docker options or filesystem spec.
     */
    public DockerSandboxConfigurer(
            SandboxSnapshotProvider snapshotProvider,
            SandboxClient<DockerSandboxClientOptions> sandboxClient) {
        this.snapshotProvider = snapshotProvider;
        this.sandboxClient = sandboxClient;
    }

    /** Creates the per-call host workspace projection safety check. */
    public static Runnable workspaceProjectionPreflight(HarnessFilesystemContext context) {
        Objects.requireNonNull(context, "context");
        DockerSandboxConfig config = Objects.requireNonNull(
                context.agentConfig().getHarness().getDocker(),
                "liteflow.agent.harness.docker must not be null");
        config.validate();
        return new DockerWorkspaceProjectionPreflight(
                context.workspaceRoot(),
                config.isWorkspaceProjectionEnabled(),
                config.getWorkspaceProjectionRoots());
    }

    @Override
    public void configure(HarnessAgent.Builder builder, HarnessFilesystemContext context) {
        Objects.requireNonNull(builder, "builder");
        Objects.requireNonNull(context, "context");
        DockerSandboxConfig config = Objects.requireNonNull(
                context.agentConfig().getHarness().getDocker(),
                "liteflow.agent.harness.docker must not be null");
        config.validate();
        SandboxSnapshotSpec snapshot = resolveSnapshot(config, context);

        DockerFilesystemSpec spec = new DockerFilesystemSpec()
                .image(config.getImage())
                .workspaceRoot(config.getWorkspaceRoot())
                .memorySizeBytes(config.getMemorySizeBytes())
                .cpuCount(config.getCpuCount())
                .network(config.getNetwork().getDockerValue())
                .additionalRunArgs(REQUIRED_RUN_ARGS)
                .snapshotSpec(snapshot);
        if (config.isWorkspaceProjectionEnabled()) {
            SandboxClient<DockerSandboxClientOptions> effectiveClient = sandboxClient != null
                    ? sandboxClient
                    : new DockerSandboxClient();
            spec.client(new ProjectionValidatingSandboxClient<>(
                    effectiveClient, workspaceProjectionPreflight(context)));
        }
        else if (sandboxClient != null) {
            spec.client(sandboxClient);
        }
        spec.isolationScope(IsolationScope.SESSION);
        spec.workspaceProjectionEnabled(config.isWorkspaceProjectionEnabled());
        spec.workspaceProjectionRoots(config.getWorkspaceProjectionRoots());
        builder.filesystem(spec);
    }

    private SandboxSnapshotSpec resolveSnapshot(
            DockerSandboxConfig config, HarnessFilesystemContext context) {
        String root = config.getSnapshotRoot();
        boolean hasLocalRoot = root != null && !root.isBlank();
        if (hasLocalRoot && snapshotProvider != null) {
            throw new IllegalStateException(
                    "liteflow.agent.harness.docker.snapshot-root conflicts with "
                            + "SandboxSnapshotProvider");
        }
        if (snapshotProvider != null) {
            SandboxSnapshotSpec snapshot = snapshotProvider.provide(context);
            if (snapshot == null) {
                throw new IllegalStateException("SandboxSnapshotProvider must not return null");
            }
            return snapshot;
        }
        if (!hasLocalRoot) {
            return new NoopSnapshotSpec();
        }
        return new LocalSnapshotSpec(Path.of(root));
    }
}
