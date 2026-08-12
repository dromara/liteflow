package com.yomahub.liteflow.agent.harness.runtime;

import com.yomahub.liteflow.agent.runtime.AgentRuntimeOwnership;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

/** Owns one component-level HarnessAgent and its LiteFlow-managed resources. */
public final class HarnessAgentRuntime implements AutoCloseable {

    private final HarnessAgent agent;
    private final AgentRuntimeOwnership ownership;
    private final List<? extends AutoCloseable> ownedHarnessResources;
    private final SandboxCallGate sandboxCallGate;

    public HarnessAgentRuntime(
            HarnessAgent agent,
            AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources) {
        this(agent, ownership, ownedHarnessResources, null);
    }

    public HarnessAgentRuntime(
            HarnessAgent agent,
            AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources,
            SandboxCallGate sandboxCallGate) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.ownedHarnessResources = List.copyOf(Objects.requireNonNull(
                ownedHarnessResources, "ownedHarnessResources"));
        this.sandboxCallGate = sandboxCallGate;
    }

    public HarnessAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return ownership.stateStore();
    }

    /** Runs one full logical invocation under the Docker middleware gate, when configured. */
    public <T> Mono<T> executeSandboxCall(Supplier<Mono<T>> invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return sandboxCallGate == null
                ? Mono.defer(invocation)
                : sandboxCallGate.execute(invocation);
    }

    @Override
    public void close() {
        if (sandboxCallGate != null) {
            sandboxCallGate.close();
        }
        ownership.close("Harness Agent runtime", agent, ownedHarnessResources);
    }
}
