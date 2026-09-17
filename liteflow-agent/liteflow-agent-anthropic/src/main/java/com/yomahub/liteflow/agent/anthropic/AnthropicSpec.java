package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicBaseFormatter;

import java.util.LinkedHashMap;
import java.util.Map;
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
        return recordMetadata(buildModel(credential.apiKey(), credential.baseUrl()), "anthropic", credential.baseUrl());
    }

    protected Model buildModel(String apiKey, String baseUrl) {
        ThinkingAwareBuilder builder = new ThinkingAwareBuilder();
        builder.apiKey(apiKey).baseUrl(baseUrl).modelName(modelName);
        GenerateOptions options = normalizeThinkingOptions(
                mergeGenerateOptions(buildGenerateOptions()));
        if (options != null) {
            builder.defaultOptions(options);
            if (options.getStream() != null) {
                builder.stream(options.getStream());
            }
        }
        builder.formatter(formatter);
        if (builderCustomizer != null) {
            builderCustomizer.accept(builder);
        }
        AnthropicClientBridge.verifyContract();
        return AnthropicClientOwner.own(builder.buildManaged());
    }

    private GenerateOptions buildGenerateOptions() {
        if (Boolean.FALSE.equals(thinkingEnabled) || thinkingBudget == null) {
            return null;
        }
        return GenerateOptions.builder()
                .thinkingBudget(thinkingBudget)
                .build();
    }

    private GenerateOptions normalizeThinkingOptions(GenerateOptions options) {
        if (options == null) {
            if (Boolean.TRUE.equals(thinkingEnabled)) {
                throw invalidThinkingBudget();
            }
            return null;
        }

        Map<String, Object> bodyParams =
                new LinkedHashMap<>(options.getAdditionalBodyParams());
        if (Boolean.FALSE.equals(thinkingEnabled)) {
            bodyParams.remove("thinking");
            return copyWithThinking(options, null, bodyParams);
        }
        if (!bodyParams.containsKey("thinking") && options.getThinkingBudget() != null) {
            bodyParams.put("thinking", Map.of(
                    "type", "enabled", "budget_tokens", options.getThinkingBudget()));
        }
        GenerateOptions normalized = copyWithThinking(
                options, options.getThinkingBudget(), bodyParams);
        AnthropicThinkingFormatter.select(null, normalized);
        if (Boolean.TRUE.equals(thinkingEnabled)
                && normalized.getThinkingBudget() == null
                && !normalized.getAdditionalBodyParams().containsKey("thinking")) {
            throw invalidThinkingBudget();
        }
        return normalized;
    }

    private static GenerateOptions copyWithThinking(
            GenerateOptions options,
            Integer thinkingBudget,
            Map<String, Object> bodyParams) {
        return GenerateOptions.builder()
                .apiKey(options.getApiKey())
                .baseUrl(options.getBaseUrl())
                .endpointPath(options.getEndpointPath())
                .modelName(options.getModelName())
                .stream(options.getStream())
                .temperature(options.getTemperature())
                .topP(options.getTopP())
                .maxTokens(options.getMaxTokens())
                .maxCompletionTokens(options.getMaxCompletionTokens())
                .frequencyPenalty(options.getFrequencyPenalty())
                .presencePenalty(options.getPresencePenalty())
                .thinkingBudget(thinkingBudget)
                .reasoningEffort(options.getReasoningEffort())
                .executionConfig(options.getExecutionConfig())
                .toolChoice(options.getToolChoice())
                .topK(options.getTopK())
                .seed(options.getSeed())
                .cacheControl(options.getCacheControl())
                .parallelToolCalls(options.getParallelToolCalls())
                .responseFormat(options.getResponseFormat())
                .additionalHeaders(options.getAdditionalHeaders())
                .additionalBodyParams(bodyParams)
                .additionalQueryParams(options.getAdditionalQueryParams())
                .build();
    }

    private static AgentConfigException invalidThinkingBudget() {
        return new AgentConfigException(
                "Anthropic thinking requires a budget when enabled");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Keeps the request-boundary decorator when the last-running customizer replaces formatter. */
    private static final class ThinkingAwareBuilder extends AnthropicChatModel.Builder {
        private boolean built;

        @Override
        public AnthropicChatModel.Builder formatter(AnthropicBaseFormatter formatter) {
            if (formatter instanceof AnthropicThinkingFormatter) {
                return super.formatter(formatter);
            }
            return super.formatter(new AnthropicThinkingFormatter(formatter));
        }

        @Override
        public AnthropicChatModel build() {
            throw escapedBuild();
        }

        private AnthropicChatModel buildManaged() {
            if (built) {
                throw escapedBuild();
            }
            built = true;
            return super.build();
        }

        private static AgentConfigException escapedBuild() {
            return new AgentConfigException(
                    "customizeBuilder cannot call or retain builder.build(); "
                            + "LiteFlow owns Anthropic model construction");
        }
    }
}
