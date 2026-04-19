package com.yomahub.liteflow.property.agent;

public class WorkspaceConfig {
    private String root;
    private boolean autoCreate = true;
    private boolean cleanupOnSessionExpire = true;
    private boolean cleanupOnJvmShutdown = false;
    private long maxFileBytes = 10L * 1024 * 1024;
    private int maxListSize = 1000;

    public String getRoot() { return root; }
    public void setRoot(String root) { this.root = root; }
    public boolean isAutoCreate() { return autoCreate; }
    public void setAutoCreate(boolean autoCreate) { this.autoCreate = autoCreate; }
    public boolean isCleanupOnSessionExpire() { return cleanupOnSessionExpire; }
    public void setCleanupOnSessionExpire(boolean v) { this.cleanupOnSessionExpire = v; }
    public boolean isCleanupOnJvmShutdown() { return cleanupOnJvmShutdown; }
    public void setCleanupOnJvmShutdown(boolean v) { this.cleanupOnJvmShutdown = v; }
    public long getMaxFileBytes() { return maxFileBytes; }
    public void setMaxFileBytes(long v) { this.maxFileBytes = v; }
    public int getMaxListSize() { return maxListSize; }
    public void setMaxListSize(int v) { this.maxListSize = v; }
}
