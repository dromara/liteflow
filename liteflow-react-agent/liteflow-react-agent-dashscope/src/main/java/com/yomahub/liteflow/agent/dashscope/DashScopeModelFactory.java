package com.yomahub.liteflow.agent.dashscope;

import io.agentscope.core.model.DashScopeChatModel;

public final class DashScopeModelFactory {
    private DashScopeModelFactory() {}

    public static DashScopeChatModel of(String apiKey, String modelName) {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}