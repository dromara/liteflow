package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.slot.Slot;

import java.nio.file.Path;
import java.util.Objects;

public class ReActAgentContext {
    private final Slot slot;
    private final String sessionId;
    private final Path workspaceDir;

    public ReActAgentContext(Slot slot, String sessionId, Path workspaceDir) {
        this.slot = Objects.requireNonNull(slot, "slot");
        this.sessionId = Objects.requireNonNull(sessionId, "sessionId");
        this.workspaceDir = Objects.requireNonNull(workspaceDir, "workspaceDir");
    }

    public Slot getSlot() { return slot; }
    public String getSessionId() { return sessionId; }
    public Path getWorkspaceDir() { return workspaceDir; }
}
