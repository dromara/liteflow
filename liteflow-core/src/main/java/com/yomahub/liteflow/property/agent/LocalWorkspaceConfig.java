package com.yomahub.liteflow.property.agent;

/** Host execution location. Ignored by the Docker backend. */
public class LocalWorkspaceConfig {
    private String workspaceRoot;

    public String getWorkspaceRoot() { return workspaceRoot; }
    public void setWorkspaceRoot(String workspaceRoot) { this.workspaceRoot = workspaceRoot; }
}
