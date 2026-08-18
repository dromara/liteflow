package com.yomahub.liteflow.property.agent;

/**
 * Supported AgentScope 2 state-store sources.
 *
 * <p>Every backend is persistent: conversation memory must survive restarts, so a
 * non-persistent in-memory option is deliberately not offered.
 */
public enum AgentStateStoreType {
	/** Local JSON files, one directory per (userId, sessionId) slot. */
	JSON,
	/** Redis via {@code agentscope-extensions-redis}; requires the liteflow-react-agent-redis module. */
	REDIS,
	/** MySQL via {@code agentscope-extensions-mysql}; requires the liteflow-react-agent-mysql module. */
	MYSQL
}
