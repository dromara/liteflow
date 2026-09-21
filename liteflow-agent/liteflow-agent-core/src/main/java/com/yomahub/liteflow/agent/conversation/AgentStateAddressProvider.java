package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;

/** ServiceLoader extension for locating pre-existing provider state without building an Agent. */
public interface AgentStateAddressProvider {
    String sessionId(AgentInvocationIdentity identity);
}
