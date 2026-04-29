package com.yomahub.liteflow.agent.gemini;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.GeminiChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.function.Consumer;

public class GeminiSpec extends ModelSpec<GeminiSpec> {

    private final String modelName;
    private String  thinkingLevel;
    private Integer thinkingBudget;

    public GeminiSpec(String modelName) { this.modelName = modelName; }

    public GeminiSpec thinking(Consumer<GeminiThinking> c) {
        GeminiThinking t = new GeminiThinking();
        c.accept(t);
        this.thinkingLevel  = t.getLevel();
        this.thinkingBudget = t.getBudget();
        return this;
    }

    public String  getModelName()      { return modelName; }
    public String  getThinkingLevel()  { return thinkingLevel; }
    public Integer getThinkingBudget() { return thinkingBudget; }

    @Override
    public Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireFirstClass(
                cfg.getGemini(), "liteflow.agent.gemini");

        GeminiChatModel.Builder builder = GeminiChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName);
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.defaultOptions(options);
        }
        if (getStream() != null) {
            builder.streamEnabled(getStream());
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && thinkingLevel == null && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)   b.temperature(getTemperature());
        if (getTopP() != null)          b.topP(getTopP());
        if (getTopK() != null)          b.topK(getTopK());
        if (getMaxTokens() != null)     b.maxTokens(getMaxTokens());
        if (getSeed() != null)          b.seed(getSeed());
        if (thinkingLevel != null)      b.reasoningEffort(thinkingLevel);
        if (thinkingBudget != null)     b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}
