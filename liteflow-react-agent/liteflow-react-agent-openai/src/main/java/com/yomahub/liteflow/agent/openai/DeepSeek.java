package com.yomahub.liteflow.agent.openai;

public final class DeepSeek {
    private DeepSeek() {}

    public static OpenAIProviderSpec of(String modelName) {
        return new OpenAIProviderSpec("deepseek", "deepseek", modelName);
    }
}
