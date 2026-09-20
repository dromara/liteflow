package com.yomahub.liteflow.agent.openai;

public final class GLM {
    private GLM() {}

    public static OpenAIProviderSpec of(String modelName) {
        return new OpenAIProviderSpec("glm", "glm", modelName);
    }
}
