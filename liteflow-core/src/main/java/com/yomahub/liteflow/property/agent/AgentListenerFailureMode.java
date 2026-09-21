package com.yomahub.liteflow.property.agent;

/** Behavior when a LiteFlow listener cannot consume an agent event. */
public enum AgentListenerFailureMode {
	FAIL_FAST,
	LOG_AND_CONTINUE
}
