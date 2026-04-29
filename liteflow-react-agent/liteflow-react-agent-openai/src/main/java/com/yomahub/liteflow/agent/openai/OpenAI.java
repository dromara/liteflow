package com.yomahub.liteflow.agent.openai;

/**
 * OpenAI 官方 API 入口。credential 来源：{@code liteflow.agent.openai}。
 */
public final class OpenAI {

    private OpenAI() {}

    public static OpenAISpec of(String modelName) {
        return new OpenAISpec(modelName);
    }
}
