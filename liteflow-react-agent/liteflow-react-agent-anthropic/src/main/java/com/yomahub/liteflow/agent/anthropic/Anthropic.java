package com.yomahub.liteflow.agent.anthropic;

public final class Anthropic {
    private Anthropic() {}
    public static AnthropicSpec of(String modelName) {
        return new AnthropicSpec(modelName);
    }
}
