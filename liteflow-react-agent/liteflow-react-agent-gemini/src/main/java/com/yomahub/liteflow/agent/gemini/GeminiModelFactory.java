package com.yomahub.liteflow.agent.gemini;

import io.agentscope.extensions.model.gemini.GeminiChatModel;

public final class GeminiModelFactory {
    private GeminiModelFactory() {}

    public static GeminiChatModel of(String apiKey, String modelName) {
        return GeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}
