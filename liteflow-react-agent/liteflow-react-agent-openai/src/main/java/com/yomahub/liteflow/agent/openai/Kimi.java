package com.yomahub.liteflow.agent.openai;

public final class Kimi {
    private static final String CONFIG_KEY = "kimi";
    private static final String BASE_URL = "https://api.moonshot.cn/v1";
    private Kimi() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
