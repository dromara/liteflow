package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;

/** Backend-owned, cross-process invocation coordination for AUTO mode. */
public interface AgentInvocationGuardProvider {
    AgentStateStoreType type();
    AgentInvocationGuard resolve(AgentConfig config);
}
