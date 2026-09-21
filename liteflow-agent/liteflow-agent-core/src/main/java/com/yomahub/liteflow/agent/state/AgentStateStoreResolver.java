package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;

/** Resolves the configured AgentScope state store and its resource ownership. */
@FunctionalInterface
public interface AgentStateStoreResolver {

    ResolvedAgentStateStore resolve(AgentSessionStoreConfig config);
}
