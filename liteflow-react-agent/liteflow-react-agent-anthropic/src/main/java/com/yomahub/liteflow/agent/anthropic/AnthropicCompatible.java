package com.yomahub.liteflow.agent.anthropic;

public final class AnthropicCompatible {
    private AnthropicCompatible() {}
    public static AnthropicSpec custom(String configKey, String modelName) {
        return new AnthropicSpec(modelName, configKey);
    }
}
