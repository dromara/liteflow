package com.yomahub.liteflow.agent.dashscope;

/** DashScope（通义千问）thinking 子构建器，沿用 thinking_budget 术语。 */
public final class DashScopeThinking {
    private Integer budget;

    public DashScopeThinking budget(int tokens) { this.budget = tokens; return this; }
    public Integer getBudget() { return budget; }
}
