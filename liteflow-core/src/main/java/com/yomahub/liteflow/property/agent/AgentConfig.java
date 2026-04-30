package com.yomahub.liteflow.property.agent;

import java.util.LinkedHashMap;
import java.util.Map;

public class AgentConfig {
    private WorkspaceConfig workspace = new WorkspaceConfig();
    private SessionConfig session = new SessionConfig();
    private ShellConfig shell = new ShellConfig();
    private DefaultsConfig defaults = new DefaultsConfig();
    private LoggingConfig logging = new LoggingConfig();
    private PlatformCredential openai = new PlatformCredential();
    private PlatformCredential anthropic = new PlatformCredential();
    private PlatformCredential gemini = new PlatformCredential();
    private PlatformCredential dashscope = new PlatformCredential();
    private Map<String, PlatformCredential> openaiCompatible = new LinkedHashMap<>();
    private Map<String, PlatformCredential> anthropicCompatible = new LinkedHashMap<>();

    public WorkspaceConfig getWorkspace() { return workspace; }
    public void setWorkspace(WorkspaceConfig v) { this.workspace = v; }
    public SessionConfig getSession() { return session; }
    public void setSession(SessionConfig v) { this.session = v; }
    public ShellConfig getShell() { return shell; }
    public void setShell(ShellConfig v) { this.shell = v; }
    public DefaultsConfig getDefaults() { return defaults; }
    public void setDefaults(DefaultsConfig v) { this.defaults = v; }
    public LoggingConfig getLogging() { return logging; }
    public void setLogging(LoggingConfig v) { this.logging = v; }
    public PlatformCredential getOpenai() { return openai; }
    public void setOpenai(PlatformCredential v) { this.openai = v; }
    public PlatformCredential getAnthropic() { return anthropic; }
    public void setAnthropic(PlatformCredential v) { this.anthropic = v; }
    public PlatformCredential getGemini() { return gemini; }
    public void setGemini(PlatformCredential v) { this.gemini = v; }
    public PlatformCredential getDashscope() { return dashscope; }
    public void setDashscope(PlatformCredential v) { this.dashscope = v; }
    public Map<String, PlatformCredential> getOpenaiCompatible() { return openaiCompatible; }
    public void setOpenaiCompatible(Map<String, PlatformCredential> v) { this.openaiCompatible = v; }
    public Map<String, PlatformCredential> getAnthropicCompatible() { return anthropicCompatible; }
    public void setAnthropicCompatible(Map<String, PlatformCredential> v) { this.anthropicCompatible = v; }
}
