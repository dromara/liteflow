package com.yomahub.liteflow.agent.model.catalog;

import io.agentscope.core.model.Model;

import java.net.URI;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/** Describes a resolved model without changing its identity, transport or shutdown ownership. */
public record ModelMetadata(String provider, String model, Integer contextWindow, Integer outputBudget, Boolean completionTokens) {
    private static final ReferenceQueue<Model> QUEUE = new ReferenceQueue<>();
    private static final Map<IdentityReference, ModelMetadata> MODELS = new HashMap<>();

    public static synchronized Model register(Model model, String provider, Integer contextWindow, Integer outputBudget,
            Boolean completionTokens) {
        clean();
        if (model != null) MODELS.put(new IdentityReference(model, QUEUE),
                new ModelMetadata(provider, model.getModelName(), contextWindow, outputBudget, completionTokens));
        return model;
    }

    public static synchronized ModelMetadata of(Model model) {
        clean();
        return MODELS.get(new IdentityReference(model, null));
    }

    private static void clean() {
        IdentityReference reference;
        while ((reference = (IdentityReference) QUEUE.poll()) != null) MODELS.remove(reference);
    }

    private static final class IdentityReference extends WeakReference<Model> {
        private final int hash;
        IdentityReference(Model model, ReferenceQueue<Model> queue) {
            super(model, queue);
            hash = System.identityHashCode(model);
        }
        @Override public int hashCode() { return hash; }
        @Override public boolean equals(Object other) {
            return this == other || (other instanceof IdentityReference reference && get() != null && get() == reference.get());
        }
    }

    /** A custom endpoint must not inherit another host's capacity just because names match. */
    public static String providerFor(String defaultProvider, String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) return normalize(defaultProvider);
        String host;
        try { host = URI.create(baseUrl).getHost(); }
        catch (IllegalArgumentException invalid) { return null; }
        if (host == null) return null;
        return switch (host.toLowerCase(java.util.Locale.ROOT)) {
            case "api.openai.com" -> "openai";
            case "api.anthropic.com" -> "anthropic";
            case "generativelanguage.googleapis.com" -> "google";
            case "api.deepseek.com" -> "deepseek";
            case "dashscope.aliyuncs.com" -> "alibaba-cn";
            case "dashscope-intl.aliyuncs.com", "dashscope-us.aliyuncs.com" -> "alibaba";
            case "api.moonshot.cn" -> "moonshotai-cn";
            case "api.moonshot.ai" -> "moonshotai";
            case "api.minimax.io" -> "minimax";
            case "api.minimax.chat", "api.minimaxi.com" -> "minimax-cn";
            case "openrouter.ai" -> "openrouter";
            case "api.z.ai" -> baseUrl.contains("/coding/") ? "zai-coding-plan" : "zai";
            case "open.bigmodel.cn" -> baseUrl.contains("/coding/") ? "zhipuai-coding-plan" : "zhipuai";
            default -> null;
        };
    }

    private static String normalize(String provider) {
        if (provider == null) return null;
        return switch (provider) {
            case "dashscope" -> "alibaba-cn";
            case "gemini" -> "google";
            case "kimi" -> "moonshotai-cn";
            case "glm" -> "zhipuai";
            case "minimax" -> "minimax-cn";
            default -> provider;
        };
    }
}
