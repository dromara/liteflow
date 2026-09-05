package com.yomahub.liteflow.agent.anthropic;

import io.agentscope.extensions.model.anthropic.AnthropicChatModel;

public final class AnthropicModelFactory {
    private AnthropicModelFactory() {}

    public static AnthropicClientOwner of(String apiKey, String modelName) {
        AnthropicClientBridge.verifyContract();
        return AnthropicClientOwner.own(AnthropicChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName)
                .formatter(new AnthropicThinkingFormatter(null))
                .build());
    }

    public static AnthropicClientOwner custom(
            String apiKey, String baseUrl, String modelName) {
        AnthropicClientBridge.verifyContract();
        return AnthropicClientOwner.own(AnthropicChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .formatter(new AnthropicThinkingFormatter(null))
                .build());
    }
}
