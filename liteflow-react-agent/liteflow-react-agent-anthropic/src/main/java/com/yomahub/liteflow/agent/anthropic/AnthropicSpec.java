package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicBaseFormatter;

import java.util.function.Consumer;

public class AnthropicSpec extends ModelSpec<AnthropicSpec> {

    private final String modelName;
    private Integer thinkingBudget;
    private Boolean thinkingEnabled;
    private AnthropicBaseFormatter formatter;
    private Consumer<AnthropicChatModel.Builder> builderCustomizer;

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
        this.thinkingEnabled = t.getEnabled() == null
                && t.getBudget() != null && t.getBudget() > 0
                ? Boolean.TRUE
                : t.getEnabled();
        return this;
    }

    public AnthropicSpec formatter(AnthropicBaseFormatter formatter) {
        this.formatter = formatter;
        return this;
    }

    public AnthropicSpec customizeBuilder(Consumer<AnthropicChatModel.Builder> customizer) {
        this.builderCustomizer = customizer;
        return this;
    }

    public String getModelName()         { return modelName; }
    public Integer getThinkingBudget()   { return thinkingBudget; }
    public Boolean getThinkingEnabled()  { return thinkingEnabled; }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential;
        if (compatibleConfigKey == null) {
            credential = CredentialResolver.resolveFirstClass(
                    cfg.getAnthropic(),
                    "liteflow.agent.anthropic",
                    getApiKey(),
                    getBaseUrl());
        } else {
            credential = CredentialResolver.resolveCompatible(
                    cfg.getAnthropicCompatible(),
                    compatibleConfigKey,
                    "liteflow.agent.anthropic-compatible",
                    getApiKey(),
                    getBaseUrl());
            if (isBlank(credential.baseUrl())) {
                throw new AgentConfigException(
                        "Missing base URL: please configure liteflow.agent.anthropic-compatible."
                                + compatibleConfigKey + ".base-url");
            }
        }
        return buildModel(credential.apiKey(), credential.baseUrl());
    }

    protected Model buildModel(String apiKey, String baseUrl) {
        AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName);
        GenerateOptions options = mergeGenerateOptions(buildGenerateOptions());
        if (options != null) {
            builder.defaultOptions(options);
            if (options.getStream() != null) {
                builder.stream(options.getStream());
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
        Integer effectiveThinkingBudget = effectiveThinkingBudget();
        if (effectiveThinkingBudget == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        b.thinkingBudget(effectiveThinkingBudget);
        return b.build();
    }

    private Integer effectiveThinkingBudget() {
        if (Boolean.FALSE.equals(thinkingEnabled)) {
            return null;
        }
        if (thinkingBudget == null || thinkingBudget <= 0) {
            if (Boolean.TRUE.equals(thinkingEnabled) || thinkingBudget != null) {
                throw new AgentConfigException(
                        "Anthropic thinking requires a positive token budget when enabled");
            }
            return null;
        }
        return thinkingBudget;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
