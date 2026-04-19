package com.yomahub.liteflow.property.agent;

import java.time.Duration;

public class SessionConfig {
    private Duration idleTimeout = Duration.ofMinutes(30);
    private Duration cleanupInterval = Duration.ofSeconds(10);
    private int maxSessions = 100;

    public Duration getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(Duration idleTimeout) { this.idleTimeout = idleTimeout; }

    public Duration getCleanupInterval() { return cleanupInterval; }
    public void setCleanupInterval(Duration cleanupInterval) { this.cleanupInterval = cleanupInterval; }

    public int getMaxSessions() { return maxSessions; }
    public void setMaxSessions(int maxSessions) { this.maxSessions = maxSessions; }
}
