package com.yomahub.liteflow.agent.dashscope;

import io.agentscope.extensions.model.dashscope.DashScopeChatModel;

public final class DashScopeModelFactory {
    private DashScopeModelFactory() {}

    public static DashScopeChatModel of(String apiKey, String modelName) {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    public static DashScopeChatModel custom(String apiKey, String baseUrl, String modelName) {
        return DashScopeChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}
