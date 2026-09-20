package com.yomahub.liteflow.agent.exception;

public class AgentInvocationException extends AgentException {
    private final AgentInvocationErrorType errorType;

    public AgentInvocationException(String message) {
        this(AgentInvocationErrorType.ACQUISITION_FAILED, message);
    }

    public AgentInvocationException(String message, Throwable cause) {
        this(AgentInvocationErrorType.ACQUISITION_FAILED, message, cause);
    }

    public AgentInvocationException(AgentInvocationErrorType errorType, String message) {
        super(message);
        this.errorType = java.util.Objects.requireNonNull(errorType, "errorType");
    }

    public AgentInvocationException(AgentInvocationErrorType errorType, String message, Throwable cause) {
        super(message, cause);
        this.errorType = java.util.Objects.requireNonNull(errorType, "errorType");
    }

    public AgentInvocationErrorType getErrorType() {
        return errorType;
    }
}
