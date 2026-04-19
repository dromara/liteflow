package com.yomahub.liteflow.agent.anthropic;

import io.agentscope.core.model.AnthropicChatModel;

public final class AnthropicModelFactory {
    private AnthropicModelFactory() {}

    public static AnthropicChatModel of(String apiKey, String modelName) {
        return AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .build();
    }
}