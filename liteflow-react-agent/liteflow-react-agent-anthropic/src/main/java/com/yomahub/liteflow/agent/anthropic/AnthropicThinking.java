package com.yomahub.liteflow.agent.anthropic;

/** Anthropic 平台 thinking 子构建器。沿用 Anthropic 原生术语。 */
public final class AnthropicThinking {
    private Integer budget;
    private Boolean enabled;

    public AnthropicThinking budget(int tokens) { this.budget = tokens; return this; }
    public AnthropicThinking enabled(boolean v) { this.enabled = v;     return this; }

    public Integer getBudget() { return budget; }
    public Boolean getEnabled() { return enabled; }
}
