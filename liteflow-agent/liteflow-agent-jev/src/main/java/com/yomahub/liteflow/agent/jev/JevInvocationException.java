package com.yomahub.liteflow.agent.jev;

/** A transport or protocol failure, distinct from an uncertain business decision. */
public class JevInvocationException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    private final int statusCode;

    public JevInvocationException(String message) {
        this(message, 0, null);
    }

    public JevInvocationException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    /** HTTP status, or zero when there was no HTTP error response. */
    public int getStatusCode() {
        return statusCode;
    }
}
