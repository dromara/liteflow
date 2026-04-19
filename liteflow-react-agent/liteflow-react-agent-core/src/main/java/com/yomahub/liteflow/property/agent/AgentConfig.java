package com.yomahub.liteflow.property.agent;

public class AgentConfig {
    private WorkspaceConfig workspace = new WorkspaceConfig();
    private SessionConfig session = new SessionConfig();

    public WorkspaceConfig getWorkspace() { return workspace; }
    public void setWorkspace(WorkspaceConfig workspace) { this.workspace = workspace; }

    public SessionConfig getSession() { return session; }
    public void setSession(SessionConfig session) { this.session = session; }
}
