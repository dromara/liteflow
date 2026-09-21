package com.yomahub.liteflow.agent.harness.sandbox;

import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.harness.agent.sandbox.ExecResult;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxClientOptions;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

import java.io.InputStream;
import java.util.Objects;

/** Runs the host projection policy immediately before upstream starts a sandbox. */
final class ProjectionValidatingSandboxClient<O extends SandboxClientOptions>
        implements SandboxClient<O> {

    private final SandboxClient<O> delegate;
    private final Runnable projectionPreflight;

    ProjectionValidatingSandboxClient(
            SandboxClient<O> delegate, Runnable projectionPreflight) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
        this.projectionPreflight = Objects.requireNonNull(
                projectionPreflight, "projectionPreflight");
    }

    @Override
    public Sandbox create(
            WorkspaceSpec workspaceSpec,
            SandboxSnapshotSpec snapshotSpec,
            O options) {
        return wrap(delegate.create(workspaceSpec, snapshotSpec, options));
    }

    @Override
    public Sandbox resume(SandboxState state) {
        return wrap(delegate.resume(state));
    }

    @Override
    public void delete(Sandbox sandbox) {
        delegate.delete(unwrap(sandbox));
    }

    @Override
    public String serializeState(SandboxState state) {
        return delegate.serializeState(state);
    }

    @Override
    public SandboxState deserializeState(String json) {
        return delegate.deserializeState(json);
    }

    @Override
    public SandboxState deserializeState(
            String json, SandboxSnapshotSpec snapshotSpec) {
        return delegate.deserializeState(json, snapshotSpec);
    }

    private Sandbox wrap(Sandbox sandbox) {
        return new ProjectionValidatingSandbox(sandbox, projectionPreflight);
    }

    private static Sandbox unwrap(Sandbox sandbox) {
        if (sandbox instanceof ProjectionValidatingSandbox) {
            return ((ProjectionValidatingSandbox) sandbox).delegate;
        }
        return sandbox;
    }

    private static final class ProjectionValidatingSandbox implements Sandbox {

        private final Sandbox delegate;
        private final Runnable projectionPreflight;

        private ProjectionValidatingSandbox(Sandbox delegate, Runnable projectionPreflight) {
            this.delegate = Objects.requireNonNull(delegate, "sandbox");
            this.projectionPreflight = projectionPreflight;
        }

        @Override
        public void start() throws Exception {
            projectionPreflight.run();
            delegate.start();
        }

        @Override
        public void stop() throws Exception {
            delegate.stop();
        }

        @Override
        public void shutdown() throws Exception {
            delegate.shutdown();
        }

        @Override
        public void close() throws Exception {
            delegate.close();
        }

        @Override
        public boolean isRunning() {
            return delegate.isRunning();
        }

        @Override
        public SandboxState getState() {
            return delegate.getState();
        }

        @Override
        public ExecResult exec(
                RuntimeContext context, String command, Integer timeoutSeconds) throws Exception {
            return delegate.exec(context, command, timeoutSeconds);
        }

        @Override
        public InputStream persistWorkspace() throws Exception {
            return delegate.persistWorkspace();
        }

        @Override
        public void hydrateWorkspace(InputStream archive) throws Exception {
            delegate.hydrateWorkspace(archive);
        }
    }
}
