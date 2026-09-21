package com.yomahub.liteflow.agent.harness.sandbox;

import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

/** Applies current deployment options when recreating a container from persisted metadata. */
final class PolicyBoundDockerSandboxClient implements SandboxClient<DockerSandboxClientOptions> {
    private final SandboxClient<DockerSandboxClientOptions> delegate;
    private final DockerSandboxClientOptions options;

    PolicyBoundDockerSandboxClient(SandboxClient<DockerSandboxClientOptions> delegate,
                                  DockerSandboxClientOptions options) {
        this.delegate = delegate;
        this.options = options;
    }

    @Override
    public Sandbox create(WorkspaceSpec workspace, SandboxSnapshotSpec snapshot, DockerSandboxClientOptions options) {
        return delegate.create(workspace, snapshot, options);
    }

    @Override
    public Sandbox resume(SandboxState state) {
        if (state instanceof DockerSandboxState docker) {
            docker.setImage(options.getImage());
            docker.setWorkspaceRoot(options.getWorkspaceRoot());
            docker.setMemorySizeBytes(options.getMemorySizeBytes());
            docker.setCpuCount(options.getCpuCount());
            docker.setNetwork(options.getNetwork());
            docker.setAdditionalRunArgs(options.getAdditionalRunArgs());
        }
        return delegate.resume(state);
    }

    @Override public void delete(Sandbox sandbox) { delegate.delete(sandbox); }
    @Override public String serializeState(SandboxState state) { return delegate.serializeState(state); }
    @Override public SandboxState deserializeState(String json) { return delegate.deserializeState(json); }
    @Override public SandboxState deserializeState(String json, SandboxSnapshotSpec snapshot) {
        return delegate.deserializeState(json, snapshot);
    }
}
