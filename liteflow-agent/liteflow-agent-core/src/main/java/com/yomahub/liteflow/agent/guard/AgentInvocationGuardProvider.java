package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;

/** Backend-owned, cross-process invocation coordination for AUTO mode. */
public interface AgentInvocationGuardProvider {
    AgentSessionStoreType type();
    AgentInvocationGuard resolve(AgentConfig config);
}
