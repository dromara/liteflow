package com.yomahub.liteflow.property.agent;

/**
 * Agent 全局默认值配置，对应配置段 {@code liteflow.agent.defaults.*}。
 */
public class DefaultsConfig {

    /**
     * ReAct 流程的默认最大迭代次数（即 reason → act 的循环上限）。
     *
     * <p>仅在 {@code ReActAgentComponent#maxIterations()} 未显式覆盖（返回值 ≤ 0）时生效，
     * 用于防止 LLM 陷入死循环造成无限调用。
     */
    private int maxIterations = 50;

    public int getMaxIterations() {
        return maxIterations;
    }

    public void setMaxIterations(int v) {
        this.maxIterations = v;
    }
}
