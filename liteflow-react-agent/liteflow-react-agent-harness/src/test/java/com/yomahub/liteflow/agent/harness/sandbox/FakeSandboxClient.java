package com.yomahub.liteflow.agent.harness.sandbox;

import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/** Test-only Docker-client substitute; it never invokes Docker, a process, or the network. */
public final class FakeSandboxClient implements SandboxClient<DockerSandboxClientOptions> {

    private static final String FIELD_SEPARATOR = "\n";
    private static final AtomicInteger NEXT_ID = new AtomicInteger();

    private final List<String> events;
    private final List<FakeSandbox> sandboxes = new CopyOnWriteArrayList<>();
    private final List<StateSnapshotIdentity> createdStates = new CopyOnWriteArrayList<>();
    private final List<StateSnapshotIdentity> resumedStates = new CopyOnWriteArrayList<>();
    private final List<StateSnapshotIdentity> deserializedWithSnapshotStates =
            new CopyOnWriteArrayList<>();

    public FakeSandboxClient(List<String> events) {
        this.events = Objects.requireNonNull(events, "events");
    }

    @Override
    public Sandbox create(
            WorkspaceSpec workspaceSpec,
            SandboxSnapshotSpec snapshotSpec,
            DockerSandboxClientOptions options) {
        String sessionId = "fake-" + NEXT_ID.incrementAndGet();
        DockerSandboxState state = new DockerSandboxState();
        state.setSessionId(sessionId);
        state.setWorkspaceSpec(workspaceSpec);
        state.setWorkspaceRoot(options.getWorkspaceRoot());
        state.setImage(options.getImage());
        state.setWorkspaceRootReady(false);
        if (snapshotSpec != null) {
            state.setSnapshot(snapshotSpec.build(sessionId));
        }
        createdStates.add(identity(state));
        events.add("acquire:" + sessionId);
        return add(state);
    }

    @Override
    public Sandbox resume(SandboxState state) {
        DockerSandboxState dockerState = (DockerSandboxState) state;
        resumedStates.add(identity(dockerState));
        events.add("acquire:" + dockerState.getSessionId());
        return add(dockerState);
    }

    @Override
    public void delete(Sandbox sandbox) {
    }

    @Override
    public String serializeState(SandboxState state) {
        String hash = state.getWorkspaceProjectionHash() == null
                ? ""
                : Base64.getUrlEncoder().encodeToString(
                        state.getWorkspaceProjectionHash().getBytes(StandardCharsets.UTF_8));
        return String.join(
                FIELD_SEPARATOR,
                state.getSessionId(),
                Boolean.toString(state.isWorkspaceRootReady()),
                hash,
                state.getWorkspaceSpec().getRoot());
    }

    @Override
    public SandboxState deserializeState(String json) {
        return decodeState(json, null);
    }

    @Override
    public SandboxState deserializeState(
            String json, SandboxSnapshotSpec snapshotSpec) {
        SandboxState state = decodeState(json, snapshotSpec);
        deserializedWithSnapshotStates.add(identity(state));
        return state;
    }

    private SandboxState decodeState(
            String json, SandboxSnapshotSpec snapshotSpec) {
        String[] fields = json.split(FIELD_SEPARATOR, -1);
        DockerSandboxState state = new DockerSandboxState();
        state.setSessionId(fields[0]);
        state.setWorkspaceRootReady(Boolean.parseBoolean(fields[1]));
        if (!fields[2].isEmpty()) {
            state.setWorkspaceProjectionHash(new String(
                    Base64.getUrlDecoder().decode(fields[2]), StandardCharsets.UTF_8));
        }
        WorkspaceSpec workspaceSpec = new WorkspaceSpec();
        workspaceSpec.setRoot(fields[3]);
        state.setWorkspaceSpec(workspaceSpec);
        state.setWorkspaceRoot(fields[3]);
        if (snapshotSpec != null) {
            state.setSnapshot(snapshotSpec.build(state.getSessionId()));
        }
        return state;
    }

    public List<StateSnapshotIdentity> createdStates() {
        return List.copyOf(createdStates);
    }

    public List<StateSnapshotIdentity> resumedStates() {
        return List.copyOf(resumedStates);
    }

    public List<StateSnapshotIdentity> deserializedWithSnapshotStates() {
        return List.copyOf(deserializedWithSnapshotStates);
    }

    List<FakeSandbox> sandboxes() {
        return List.copyOf(sandboxes);
    }

    FakeSandbox latestSandbox() {
        return sandboxes.get(sandboxes.size() - 1);
    }

    private FakeSandbox add(DockerSandboxState state) {
        FakeSandbox sandbox = new FakeSandbox(state, events);
        sandboxes.add(sandbox);
        return sandbox;
    }

    private static StateSnapshotIdentity identity(SandboxState state) {
        return new StateSnapshotIdentity(
                state.getSessionId(),
                state.getSnapshot() == null ? null : state.getSnapshot().getId());
    }

    public record StateSnapshotIdentity(String stateId, String snapshotId) {
    }
}
