package com.yomahub.liteflow.agent.openai;

/**
 * 自定义 OpenAI 兼容厂商兜底入口。
 * 用户在配置中挂 {@code liteflow.agent.openai-compatible.<configKey>}，
 * 至少要提供 api-key；base-url 也由用户配置决定（无默认值）。
 */
public final class OpenAICompatible {
    private OpenAICompatible() {}

    public static OpenAICompatibleSpec custom(String configKey, String modelName) {
        return new OpenAICompatibleSpec(configKey, modelName, null);
    }
}
