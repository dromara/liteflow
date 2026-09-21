package com.yomahub.liteflow.agent.openai;

public final class Minimax {
    private Minimax() {}

    public static OpenAIProviderSpec of(String modelName) {
        return new OpenAIProviderSpec("minimax", "minimax", modelName);
    }
}
