package com.yomahub.liteflow.agent.dashscope;

public final class DashScope {
    private DashScope() {}

    /**
     * DashScope 官方 API 入口。credential 来源：{@code liteflow.agent.dashscope}。
     */
    public static DashScopeSpec of(String modelName) {
        return new DashScopeSpec(modelName);
    }
}
