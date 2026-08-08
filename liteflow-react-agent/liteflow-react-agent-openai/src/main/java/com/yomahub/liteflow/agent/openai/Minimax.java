package com.yomahub.liteflow.agent.openai;

public final class Minimax {
    private static final String CONFIG_KEY = "minimax";
    private static final String BASE_URL = "https://api.minimax.io/v1";
    private Minimax() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
