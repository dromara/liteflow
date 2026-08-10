package com.yomahub.liteflow.property.agent;

/** Behavior when state persistence fails. */
public enum AgentStateStoreFailurePolicy {
	FAIL_FAST,
	LOG_AND_CONTINUE
}
