package com.yomahub.liteflow.agent.openai;

import io.agentscope.core.model.OpenAIChatModel;

public final class OpenAICompatiblePresets {
    private OpenAICompatiblePresets() {}

    public static OpenAIChatModel deepseek(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://api.deepseek.com/v1", modelName);
    }
    public static OpenAIChatModel kimi(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://api.moonshot.cn/v1", modelName);
    }
    public static OpenAIChatModel glm(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://open.bigmodel.cn/api/paas/v4", modelName);
    }
    public static OpenAIChatModel minimax(String apiKey, String modelName) {
        return OpenAIModelFactory.custom(apiKey, "https://api.minimax.io/v1", modelName);
    }
}