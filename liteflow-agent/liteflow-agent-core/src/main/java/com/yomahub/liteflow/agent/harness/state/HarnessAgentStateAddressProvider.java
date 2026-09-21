package com.yomahub.liteflow.agent.harness.state;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.conversation.AgentStateAddressProvider;

/** Locates persisted Harness state without constructing a Harness runtime or sandbox. */
public final class HarnessAgentStateAddressProvider implements AgentStateAddressProvider {
    @Override
    public String sessionId(AgentInvocationIdentity identity) {
        return HarnessNamespacedAgentStateStore.physicalAgentSessionId(
                identity.agentNamespace(), identity.runtimeSessionId());
    }
}
