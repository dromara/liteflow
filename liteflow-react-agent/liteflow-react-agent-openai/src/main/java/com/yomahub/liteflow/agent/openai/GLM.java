package com.yomahub.liteflow.agent.openai;

public final class GLM {
    private static final String CONFIG_KEY = "glm";
    private static final String BASE_URL = "https://open.bigmodel.cn/api/paas/v4";
    private GLM() {}

    public static OpenAICompatibleSpec of(String modelName) {
        return new OpenAICompatibleSpec(CONFIG_KEY, modelName, BASE_URL);
    }
}
