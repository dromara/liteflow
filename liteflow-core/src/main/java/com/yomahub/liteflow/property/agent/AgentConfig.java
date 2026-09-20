package com.yomahub.liteflow.property.agent;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Agent 模块的根配置对象。
 *
 * <p>对应 Spring Boot 配置段 {@code liteflow.agent.*}，作为 LiteFlow 中所有
 * agent 子配置的聚合入口；其内部字段会在 AgentScope 2 运行时组件、
 * 各 ProviderSpec（OpenAI / Anthropic / Gemini /
 * DashScope 等）以及内置工具装配（文件 / Shell 工具）中分别被读取使用。
 */
public class AgentConfig {

	/** Stable application name used to isolate Agent data; defaults to the framework application name. */
	private String applicationName;

	/** Maximum duration of one complete Agent execution, including model and tool calls. */
	private Duration executionTimeout = Duration.ofMinutes(10);

	/** Agent session persistence settings. */
	private AgentSessionStoreConfig sessionStore = new AgentSessionStoreConfig();

	/** Enable durable, display-oriented conversation history and Agent participation tracking by default. */
	private boolean conversationHistoryEnabled = true;

	public boolean isConversationHistoryEnabled() {
		return conversationHistoryEnabled;
	}

	public void setConversationHistoryEnabled(boolean conversationHistoryEnabled) {
		this.conversationHistoryEnabled = conversationHistoryEnabled;
	}

	/** Toolkit execution settings. */
	private AgentToolkitConfig toolkit = new AgentToolkitConfig();

	/** Agent event delivery settings. */
	private AgentEventConfig event = new AgentEventConfig();

	/** Cross-invocation coordination settings. */
	private AgentInvocationGuardConfig invocationGuard = new AgentInvocationGuardConfig();

	/** Human-in-the-loop confirmation settings. */
	private AgentHitlConfig hitl = new AgentHitlConfig();

	/** AgentScope Harness filesystem and sandbox settings. */
	private HarnessConfig harness = new HarnessConfig();

    /** 单次 Agent 执行的推理与工具调用循环上限，组件 maxIterations() 返回 -1 时使用。 */
    private int maxIterations = 100;

    /** 是否输出 Agent 执行、推理、工具调用和模型调用的生命周期日志。 */
    private boolean executionLogEnabled = true;

    /** Skills configuration for loading AgentSkillRepository entries from SKILL.md repositories. */
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

	public String getApplicationName() {
		return applicationName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public Duration getExecutionTimeout() {
		return executionTimeout;
	}

	public void setExecutionTimeout(Duration executionTimeout) {
		this.executionTimeout = executionTimeout;
	}

	public AgentSessionStoreConfig getSessionStore() {
		return sessionStore;
	}

	public void setSessionStore(AgentSessionStoreConfig sessionStore) {
		this.sessionStore = sessionStore;
	}

	public AgentToolkitConfig getToolkit() {
		return toolkit;
	}

	public void setToolkit(AgentToolkitConfig toolkit) {
		this.toolkit = toolkit;
	}

	public AgentEventConfig getEvent() {
		return event;
	}

	public void setEvent(AgentEventConfig event) {
		this.event = event;
	}

	public AgentInvocationGuardConfig getInvocationGuard() {
		return invocationGuard;
	}

	public void setInvocationGuard(AgentInvocationGuardConfig invocationGuard) {
		this.invocationGuard = invocationGuard;
	}

	public AgentHitlConfig getHitl() {
		return hitl;
	}

	public void setHitl(AgentHitlConfig hitl) {
		this.hitl = hitl;
	}

	public HarnessConfig getHarness() {
		return harness;
	}

	public void setHarness(HarnessConfig harness) {
		this.harness = harness;
	}

	/**
	 * Validates configuration needed by the AgentScope 2 runtime immediately before use.
	 */
	public void validateForExecution() {
		if (isBlank(applicationName)) {
			throw new IllegalStateException("liteflow.agent.application-name is required before execution");
		}
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    public boolean isExecutionLogEnabled() {
        return executionLogEnabled;
    }

    public void setExecutionLogEnabled(boolean executionLogEnabled) {
        this.executionLogEnabled = executionLogEnabled;
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
