package com.yomahub.liteflow.property.agent;

/** Policy for deferred adapter load errors; directly raised storage failures always propagate. */
public enum AgentStateStoreFailurePolicy {
	FAIL_FAST,
	LOG_AND_CONTINUE
}
