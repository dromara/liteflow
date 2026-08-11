package com.yomahub.liteflow.agent.anthropic;

public final class Anthropic {

    private Anthropic() {}

    /** Anthropic 官方 API 入口。credential 来源：{@code liteflow.agent.anthropic}。 */
    public static AnthropicSpec of(String modelName) {
        return new AnthropicSpec(modelName);
    }
}
