package com.yomahub.liteflow.agent.openai;

import io.agentscope.core.model.OpenAIChatModel;

public final class OpenAIModelFactory {
    private OpenAIModelFactory() {}

    public static OpenAIChatModel openai(String apiKey, String modelName) {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }

    public static OpenAIChatModel custom(String apiKey, String baseUrl, String modelName) {
        return OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .build();
    }
}