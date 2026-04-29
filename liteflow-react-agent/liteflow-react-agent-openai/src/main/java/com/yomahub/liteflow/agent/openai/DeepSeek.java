package com.yomahub.liteflow.agent.openai;

public final class DeepSeek {
    private static final String CONFIG_KEY = "deepseek";
    private static final String BASE_URL = "https://api.deepseek.com/v1";
    private DeepSeek() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
