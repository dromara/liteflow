package com.yomahub.liteflow.agent.anthropic;

public final class AnthropicCompatible {

    private AnthropicCompatible() {}

    /** Anthropic 兼容 API 入口。credential 来源：{@code liteflow.agent.anthropic-compatible}。 */
    public static AnthropicSpec custom(String configKey, String modelName) {
        return new AnthropicSpec(modelName, configKey);
    }
}
