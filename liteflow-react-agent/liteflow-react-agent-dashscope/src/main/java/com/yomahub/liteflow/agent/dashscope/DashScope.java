package com.yomahub.liteflow.agent.dashscope;

public final class DashScope {
    private DashScope() {}
    public static DashScopeSpec of(String modelName) {
        return new DashScopeSpec(modelName);
    }
}
