package com.yomahub.liteflow.property.agent;

import java.time.Duration;

public class SessionConfig {
    private Duration idleTimeout = Duration.ofMinutes(30);
    private Duration cleanupInterval = Duration.ofMinutes(1);
    private int maxSessions = 10_000;
    private MemoryStorageConfig memory = new MemoryStorageConfig();

    public Duration getIdleTimeout() { return idleTimeout; }
    public void setIdleTimeout(Duration v) { this.idleTimeout = v; }
    public Duration getCleanupInterval() { return cleanupInterval; }
    public void setCleanupInterval(Duration v) { this.cleanupInterval = v; }
    public int getMaxSessions() { return maxSessions; }
    public void setMaxSessions(int v) { this.maxSessions = v; }
    public MemoryStorageConfig getMemory() { return memory; }
    public void setMemory(MemoryStorageConfig memory) { this.memory = memory; }
}
