package com.yomahub.liteflow.agent.gemini;

import io.agentscope.extensions.model.gemini.GeminiChatModel;
import io.agentscope.extensions.model.gemini.formatter.GeminiChatFormatter;

public final class GeminiModelFactory {
    private GeminiModelFactory() {}

    public static OwnedGeminiModel of(String apiKey, String modelName) {
        return new OwnedGeminiModel(GeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .formatter(new GeminiThinkingFormatter(new GeminiChatFormatter()))
                .build());
    }
}
