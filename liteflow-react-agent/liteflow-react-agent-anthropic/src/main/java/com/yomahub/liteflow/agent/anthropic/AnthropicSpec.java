package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.AnthropicChatModel;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.function.Consumer;

public class AnthropicSpec extends ModelSpec<AnthropicSpec> {

    private final String modelName;
    private Integer thinkingBudget;
    private Boolean thinkingEnabled;

    /** null 表示走头等平台 (cfg.getAnthropic())；非 null 表示走 anthropic-compatible map。 */
    private final String compatibleConfigKey;

    public AnthropicSpec(String modelName) {
        this(modelName, null);
    }

    public AnthropicSpec(String modelName, String compatibleConfigKey) {
        this.modelName = modelName;
        this.compatibleConfigKey = compatibleConfigKey;
    }

    public AnthropicSpec thinking(Consumer<AnthropicThinking> c) {
        AnthropicThinking t = new AnthropicThinking();
        c.accept(t);
        this.thinkingBudget = t.getBudget();
        this.thinkingEnabled = t.getEnabled();
        return this;
    }

    public String getModelName()         { return modelName; }
    public Integer getThinkingBudget()   { return thinkingBudget; }
    public Boolean getThinkingEnabled()  { return thinkingEnabled; }

    @Override
    public Model resolve(AgentConfig cfg) {
        PlatformCredential cred;
        if (compatibleConfigKey == null) {
            cred = CredentialResolver.requireFirstClass(
                    cfg.getAnthropic(), "liteflow.agent.anthropic");
        } else {
            cred = CredentialResolver.requireCompatible(
                    cfg.getAnthropicCompatible(), compatibleConfigKey,
                    "liteflow.agent.anthropic-compatible");
        }

        AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                .apiKey(cred.getApiKey())
                .modelName(modelName);
        if (cred.getBaseUrl() != null && !cred.getBaseUrl().isBlank()) {
            builder.baseUrl(cred.getBaseUrl());
        }
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.defaultOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && thinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)  b.temperature(getTemperature());
        if (getTopP() != null)         b.topP(getTopP());
        if (getTopK() != null)         b.topK(getTopK());
        if (getMaxTokens() != null)    b.maxTokens(getMaxTokens());
        if (getSeed() != null)         b.seed(getSeed());
        if (thinkingBudget != null)    b.thinkingBudget(thinkingBudget);
        return b.build();
    }
}
