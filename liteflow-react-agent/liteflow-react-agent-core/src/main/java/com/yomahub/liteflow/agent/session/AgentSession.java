package com.yomahub.liteflow.agent.session;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class AgentSession {
    private final String sessionId;
    private final Path workspaceDir;
    private final ReentrantLock lock = new ReentrantLock();
    private volatile Object agent;
    private volatile Instant lastActive = Instant.now();

    public AgentSession(String sessionId, Path workspaceDir) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.workspaceDir = Objects.requireNonNull(workspaceDir);
    }

    public String getSessionId() { return sessionId; }
    public Path getWorkspaceDir() { return workspaceDir; }
    public ReentrantLock getLock() { return lock; }
    public Object getAgent() { return agent; }
    public void setAgent(Object agent) { this.agent = agent; }
    public Instant getLastActive() { return lastActive; }
    public void touch() { this.lastActive = Instant.now(); }
}
