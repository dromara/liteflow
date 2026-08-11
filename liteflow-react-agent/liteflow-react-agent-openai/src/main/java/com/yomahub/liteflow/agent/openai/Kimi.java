package com.yomahub.liteflow.agent.openai;

public final class Kimi {
    private Kimi() {}

    public static OpenAIProviderSpec of(String modelName) {
        return new OpenAIProviderSpec("kimi", "kimi", modelName);
    }
}
