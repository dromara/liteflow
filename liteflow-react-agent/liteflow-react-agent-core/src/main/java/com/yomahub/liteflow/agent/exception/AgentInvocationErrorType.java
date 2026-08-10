package com.yomahub.liteflow.agent.exception;

/** Classification for invocation failures that callers may handle differently. */
public enum AgentInvocationErrorType {
    TIMEOUT,
    INTERRUPTED,
    ACQUISITION_FAILED,
    PERMISSION,
    STRUCTURED_OUTPUT
}
