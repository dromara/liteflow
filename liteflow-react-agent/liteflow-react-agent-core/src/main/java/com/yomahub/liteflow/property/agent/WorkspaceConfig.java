package com.yomahub.liteflow.property.agent;

public class WorkspaceConfig {
    private String root;
    private boolean autoCreate = true;
    private boolean cleanupOnSessionExpire = true;
    private boolean cleanupOnJvmShutdown = false;

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }

    public boolean isAutoCreate() { return autoCreate; }
    public void setAutoCreate(boolean autoCreate) { this.autoCreate = autoCreate; }

    public boolean isCleanupOnSessionExpire() { return cleanupOnSessionExpire; }
    public void setCleanupOnSessionExpire(boolean cleanupOnSessionExpire) { this.cleanupOnSessionExpire = cleanupOnSessionExpire; }

    public boolean isCleanupOnJvmShutdown() { return cleanupOnJvmShutdown; }
    public void setCleanupOnJvmShutdown(boolean cleanupOnJvmShutdown) { this.cleanupOnJvmShutdown = cleanupOnJvmShutdown; }
}
