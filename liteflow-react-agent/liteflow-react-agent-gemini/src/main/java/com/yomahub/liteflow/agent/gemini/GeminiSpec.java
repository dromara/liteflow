package com.yomahub.liteflow.agent.gemini;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.gemini.GeminiChatModel;

import java.util.function.Consumer;

public class GeminiSpec extends ModelSpec<GeminiSpec> {

    private final String modelName;
    private String  thinkingLevel;
    private Integer thinkingBudget;
    private Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> formatter;
    private Consumer<GeminiChatModel.Builder> builderCustomizer;

    public GeminiSpec(String modelName) { this.modelName = modelName; }

    public GeminiSpec thinking(Consumer<GeminiThinking> c) {
        GeminiThinking t = new GeminiThinking();
        c.accept(t);
        this.thinkingLevel  = t.getLevel();
        this.thinkingBudget = t.getBudget();
        return this;
    }

    public GeminiSpec formatter(
            Formatter<Content, GenerateContentResponse, GenerateContentConfig.Builder> formatter) {
        this.formatter = formatter;
        return this;
    }

    public GeminiSpec customizeBuilder(Consumer<GeminiChatModel.Builder> customizer) {
        this.builderCustomizer = customizer;
        return this;
    }

    public String  getModelName()      { return modelName; }
    public String  getThinkingLevel()  { return thinkingLevel; }
    public Integer getThinkingBudget() { return thinkingBudget; }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential =
                CredentialResolver.resolveFirstClass(
                        cfg.getGemini(),
                        "liteflow.agent.gemini",
                        getApiKey(),
                        getBaseUrl());
        return buildModel(credential.apiKey(), credential.baseUrl());
    }

    protected Model buildModel(String apiKey, String baseUrl) {
        GeminiChatModel.Builder builder = GeminiChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        GenerateOptions options = mergeGenerateOptions(buildGenerateOptions());
        if (options != null) {
            builder.defaultOptions(options);
            if (options.getStream() != null) {
                builder.streamEnabled(options.getStream());
            }
        }
        if (formatter != null) {
            builder.formatter(formatter);
        }
        if (builderCustomizer != null) {
            builderCustomizer.accept(builder);
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (thinkingLevel == null && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (thinkingLevel != null)      b.reasoningEffort(thinkingLevel);
        if (thinkingBudget != null)     b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}
