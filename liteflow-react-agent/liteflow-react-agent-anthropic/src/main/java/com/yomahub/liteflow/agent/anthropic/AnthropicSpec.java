package com.yomahub.liteflow.agent.anthropic;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.anthropic.AnthropicChatModel;
import io.agentscope.extensions.model.anthropic.formatter.AnthropicBaseFormatter;

import java.math.BigDecimal;
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
        return buildModel(credential.apiKey(), credential.baseUrl());
    }

    protected Model buildModel(String apiKey, String baseUrl) {
        AnthropicChatModel.Builder builder = AnthropicChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName);
        GenerateOptions options = normalizeThinkingOptions(
                mergeGenerateOptions(buildGenerateOptions()));
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

        GenerateOptions nativeOptions = getGenerateOptions();
        if (nativeOptions != null
                && nativeOptions.getAdditionalBodyParams().containsKey("thinking")) {
            return normalizeExplicitThinkingBody(
                    options,
                    bodyParams,
                    nativeOptions.getAdditionalBodyParams().get("thinking"));
        }

        Integer effectiveBudget = options.getThinkingBudget();
        if (effectiveBudget != null) {
            requirePositiveBudget(effectiveBudget);
            bodyParams.put("thinking", enabledThinkingBody(effectiveBudget));
            return copyWithThinking(options, effectiveBudget, bodyParams);
        }
        if (Boolean.TRUE.equals(thinkingEnabled)) {
            throw invalidThinkingBudget();
        }

        if (bodyParams.containsKey("thinking")) {
            return normalizeExplicitThinkingBody(
                    options, bodyParams, bodyParams.get("thinking"));
        }
        return options;
    }

    private GenerateOptions normalizeExplicitThinkingBody(
            GenerateOptions options,
            Map<String, Object> bodyParams,
            Object rawThinking) {
        if (!(rawThinking instanceof Map<?, ?> thinkingBody)) {
            if (options.getThinkingBudget() != null || thinkingEnabled != null) {
                throw new AgentConfigException(
                        "Anthropic raw thinking body conflicts with typed thinking configuration");
            }
            return options;
        }

        Object type = thinkingBody.get("type");
        if ("enabled".equals(type)) {
            int budget = positiveBodyBudget(thinkingBody.get("budget_tokens"));
            Map<Object, Object> normalizedThinkingBody = new LinkedHashMap<>(thinkingBody);
            normalizedThinkingBody.put("budget_tokens", budget);
            bodyParams.put("thinking", normalizedThinkingBody);
            return copyWithThinking(options, budget, bodyParams);
        }
        if ("disabled".equals(type)) {
            return copyWithThinking(options, null, bodyParams);
        }
        if (options.getThinkingBudget() != null || thinkingEnabled != null) {
            throw new AgentConfigException(
                    "Anthropic raw thinking body conflicts with typed thinking configuration");
        }
        return options;
    }

    private int positiveBodyBudget(Object value) {
        if (!(value instanceof Number number)) {
            throw invalidThinkingBudget();
        }
        try {
            int budget = new BigDecimal(number.toString()).intValueExact();
            requirePositiveBudget(budget);
            return budget;
        } catch (ArithmeticException | NumberFormatException exception) {
            throw invalidThinkingBudget();
        }
    }

    private void requirePositiveBudget(int budget) {
        if (budget <= 0) {
            throw invalidThinkingBudget();
        }
    }

    private static Map<String, Object> enabledThinkingBody(int budget) {
        Map<String, Object> thinking = new LinkedHashMap<>();
        thinking.put("type", "enabled");
        thinking.put("budget_tokens", budget);
        return thinking;
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
                "Anthropic thinking requires a positive token budget when enabled");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
