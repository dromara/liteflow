package com.yomahub.liteflow.agent.gemini;

/**
 * Gemini 平台 thinking 子构建器。
 * Gemini 2.5 使用 "thinking_level"（low/medium/high），老接口用 "thinking_budget"（token 数）。
 */
public final class GeminiThinking {
    private String level;
    private Integer budget;

    public GeminiThinking level(String level)   { this.level = level;   return this; }
    public GeminiThinking budget(int tokens)    { this.budget = tokens; return this; }

    public String  getLevel()  { return level; }
    public Integer getBudget() { return budget; }
}
