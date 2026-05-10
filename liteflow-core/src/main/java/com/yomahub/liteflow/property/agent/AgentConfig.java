package com.yomahub.liteflow.property.agent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * ReAct Agent 模块的根配置对象。
 *
 * <p>对应 Spring Boot 配置段 {@code liteflow.agent.*}，作为 LiteFlow 中所有
 * agent 子配置的聚合入口；其内部字段会在 {@code ReActAgentComponent}、
 * {@code AgentSessionManager}、各 ProviderSpec（OpenAI / Anthropic / Gemini /
 * DashScope 等）以及工具类（{@code ManagedShellCommandTool}、
 * {@code WorkspaceFileTools}）中分别被读取使用。
 */
public class AgentConfig {

    /** 工作区配置，控制 agent 的会话工作目录、自动创建、清理策略以及文件大小上限。 */
    private WorkspaceConfig workspace = new WorkspaceConfig();

    /** 会话配置，控制内存中 agent 实例的空闲超时、清理周期、并发上限以及记忆持久化方式。 */
    private SessionConfig session = new SessionConfig();

    /** Shell 工具配置，决定 agent 调用 shell 工具时的命令过滤模式、超时与输出截断。 */
    private ShellConfig shell = new ShellConfig();

    /** 默认值配置，例如 ReAct 流程在组件未指定 maxIterations 时使用的全局默认迭代次数。 */
    private DefaultsConfig defaults = new DefaultsConfig();

    /** 日志开关配置，控制 ReAct 内部 reason / act / error 等事件日志是否输出。 */
    private LoggingConfig logging = new LoggingConfig();

    /** Skills configuration for loading agent-scope SkillBox entries from SKILL.md repositories. */
    private SkillsConfig skills = new SkillsConfig();

    /** OpenAI 头等平台凭证（{@code liteflow.agent.openai.*}），由 {@code OpenAISpec} 解析使用。 */
    private PlatformCredential openai = new PlatformCredential();

    /** Anthropic 头等平台凭证（{@code liteflow.agent.anthropic.*}），由 {@code AnthropicSpec} 解析使用。 */
    private PlatformCredential anthropic = new PlatformCredential();

    /** Gemini 头等平台凭证（{@code liteflow.agent.gemini.*}），由 {@code GeminiSpec} 解析使用。 */
    private PlatformCredential gemini = new PlatformCredential();

    /** DashScope（阿里云百炼）头等平台凭证（{@code liteflow.agent.dashscope.*}），由 {@code DashScopeSpec} 解析使用。 */
    private PlatformCredential dashscope = new PlatformCredential();

    /**
     * OpenAI 兼容平台凭证集合，key 为用户自定义平台名（如 {@code deepseek}），
     * 由 {@code OpenAICompatibleSpec} 通过 key 查找对应凭证。
     */
    private Map<String, PlatformCredential> openaiCompatible = new LinkedHashMap<>();

    /**
     * Anthropic 兼容平台凭证集合，key 为用户自定义平台名，
     * 由 {@code AnthropicSpec}（带 compatibleConfigKey）通过 key 查找对应凭证。
     */
    private Map<String, PlatformCredential> anthropicCompatible = new LinkedHashMap<>();

    public WorkspaceConfig getWorkspace() {
        return workspace;
    }

    public void setWorkspace(WorkspaceConfig v) {
        this.workspace = v;
    }

    public SessionConfig getSession() {
        return session;
    }

    public void setSession(SessionConfig v) {
        this.session = v;
    }

    public ShellConfig getShell() {
        return shell;
    }

    public void setShell(ShellConfig v) {
        this.shell = v;
    }

    public DefaultsConfig getDefaults() {
        return defaults;
    }

    public void setDefaults(DefaultsConfig v) {
        this.defaults = v;
    }

    public LoggingConfig getLogging() {
        return logging;
    }

    public void setLogging(LoggingConfig v) {
        this.logging = v;
    }

    public SkillsConfig getSkills() {
        return skills;
    }

    public void setSkills(SkillsConfig skills) {
        this.skills = skills;
    }

    public PlatformCredential getOpenai() {
        return openai;
    }

    public void setOpenai(PlatformCredential v) {
        this.openai = v;
    }

    public PlatformCredential getAnthropic() {
        return anthropic;
    }

    public void setAnthropic(PlatformCredential v) {
        this.anthropic = v;
    }

    public PlatformCredential getGemini() {
        return gemini;
    }

    public void setGemini(PlatformCredential v) {
        this.gemini = v;
    }

    public PlatformCredential getDashscope() {
        return dashscope;
    }

    public void setDashscope(PlatformCredential v) {
        this.dashscope = v;
    }

    public Map<String, PlatformCredential> getOpenaiCompatible() {
        return openaiCompatible;
    }

    public void setOpenaiCompatible(Map<String, PlatformCredential> v) {
        this.openaiCompatible = v;
    }

    public Map<String, PlatformCredential> getAnthropicCompatible() {
        return anthropicCompatible;
    }

    public void setAnthropicCompatible(Map<String, PlatformCredential> v) {
        this.anthropicCompatible = v;
    }
}
