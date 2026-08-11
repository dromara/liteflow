package com.yomahub.liteflow.agent.harness.runtime;

import com.yomahub.liteflow.agent.runtime.AgentRuntimeOwnership;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;

import java.util.List;
import java.util.Objects;

/** Owns one component-level HarnessAgent and its LiteFlow-managed resources. */
public final class HarnessAgentRuntime implements AutoCloseable {

    private final HarnessAgent agent;
    private final AgentRuntimeOwnership ownership;
    private final List<? extends AutoCloseable> ownedHarnessResources;

    public HarnessAgentRuntime(
            HarnessAgent agent,
            AgentRuntimeOwnership ownership,
            List<? extends AutoCloseable> ownedHarnessResources) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.ownership = Objects.requireNonNull(ownership, "ownership");
        this.ownedHarnessResources = List.copyOf(Objects.requireNonNull(
                ownedHarnessResources, "ownedHarnessResources"));
    }

    public HarnessAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return ownership.stateStore();
    }

    @Override
    public void close() {
        ownership.close("Harness Agent runtime", agent, ownedHarnessResources);
    }
}
