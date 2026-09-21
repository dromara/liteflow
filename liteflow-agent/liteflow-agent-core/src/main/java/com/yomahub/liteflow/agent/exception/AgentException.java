package com.yomahub.liteflow.agent.exception;

public class AgentException extends RuntimeException {
    public AgentException(String message) { super(message); }
    public AgentException(String message, Throwable cause) { super(message, cause); }
}
