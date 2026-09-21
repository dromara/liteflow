package com.yomahub.liteflow.agent.harness.sandbox;

/** Read-only, process-local observation. Timestamps are Unix epoch milliseconds. */
public record AgentSandboxStatus(
        String conversationId, String scope, State state, String containerId, String containerName,
        String image, boolean busy, Long lastActiveAt, long checkedAt, String message) {

    public enum State {
        NOT_ALLOCATED, STARTING, CREATED, RUNNING, STOPPED, PAUSED, RESTARTING, REMOVING,
        DEAD, NOT_FOUND, UNKNOWN
    }
}
