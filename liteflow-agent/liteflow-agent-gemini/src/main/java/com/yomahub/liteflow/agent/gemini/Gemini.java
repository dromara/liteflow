package com.yomahub.liteflow.agent.gemini;

public final class Gemini {
    private Gemini() {}
    public static GeminiSpec of(String modelName) {
        return new GeminiSpec(modelName);
    }
}
