package com.yomahub.liteflow.agent.openai;

public final class Minimax {
    private Minimax() {}

    /** Starts a new configuration with MiniMax-M3.1-Flash. */
    public static OpenAIProviderSpec of() {
        return of("MiniMax-M3.1-Flash");
    }

    /** Preserves explicitly selected models, including MiniMax-M3 and saved legacy IDs. */
    public static OpenAIProviderSpec of(String modelName) {
        return new OpenAIProviderSpec("minimax", "minimax", modelName, "https://api.minimax.io/v1");
    }
}
