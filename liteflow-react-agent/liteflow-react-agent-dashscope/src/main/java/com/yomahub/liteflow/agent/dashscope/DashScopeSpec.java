package com.yomahub.liteflow.agent.dashscope;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.dashscope.DashScopeChatModel;
import io.agentscope.extensions.model.dashscope.dto.DashScopeMessage;
import io.agentscope.extensions.model.dashscope.dto.DashScopeRequest;
import io.agentscope.extensions.model.dashscope.dto.DashScopeResponse;

import java.util.function.Consumer;

public class DashScopeSpec extends ModelSpec<DashScopeSpec> {

    private final String modelName;
    private Integer thinkingBudget;
    private Boolean thinkingEnabled;
    private Formatter<DashScopeMessage, DashScopeResponse, DashScopeRequest> formatter;
    private Boolean nativeStructuredOutput;
    private Boolean nativeStructuredOutputWithTools;
    private Consumer<DashScopeChatModel.Builder> builderCustomizer;

    public DashScopeSpec(String modelName) { this.modelName = modelName; }

    public DashScopeSpec thinking(Consumer<DashScopeThinking> c) {
        DashScopeThinking t = new DashScopeThinking();
        c.accept(t);
        this.thinkingBudget = t.getBudget();
        this.thinkingEnabled = t.getEnabled();
        return this;
    }

    public DashScopeSpec formatter(
            Formatter<DashScopeMessage, DashScopeResponse, DashScopeRequest> formatter) {
        this.formatter = formatter;
        return this;
    }

    public DashScopeSpec nativeStructuredOutput(boolean enabled) {
        this.nativeStructuredOutput = enabled;
        return this;
    }

    public DashScopeSpec nativeStructuredOutputWithTools(boolean enabled) {
        this.nativeStructuredOutputWithTools = enabled;
        return this;
    }

    public DashScopeSpec customizeBuilder(Consumer<DashScopeChatModel.Builder> customizer) {
        this.builderCustomizer = customizer;
        return this;
    }

    public String  getModelName()      { return modelName; }
    public Integer getThinkingBudget() { return thinkingBudget; }
    public Boolean getThinkingEnabled() { return thinkingEnabled; }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential =
                CredentialResolver.resolveFirstClass(
                        cfg.getDashscope(),
                        "liteflow.agent.dashscope",
                        getApiKey(),
                        getBaseUrl());
        return buildModel(credential.apiKey(), credential.baseUrl());
    }

    protected Model buildModel(String apiKey, String baseUrl) {
        DashScopeChatModel.Builder builder = DashScopeChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }

        GenerateOptions options = mergeGenerateOptions(buildGenerateOptions());
        if (Boolean.FALSE.equals(thinkingEnabled) && options != null) {
            options = withoutThinkingBudget(options);
        }
        Boolean enableThinking = effectiveThinking(options);
        if (options != null) {
            builder.defaultOptions(options);
            if (options.getStream() != null) {
                builder.stream(options.getStream());
            }
        }
        if (enableThinking != null) {
            builder.enableThinking(enableThinking);
        }
        if (formatter != null) {
            builder.formatter(formatter);
        }
        if (nativeStructuredOutput != null) {
            builder.nativeStructuredOutput(nativeStructuredOutput);
        }
        if (nativeStructuredOutputWithTools != null) {
            builder.nativeStructuredOutputWithTools(nativeStructuredOutputWithTools);
        }
        if (builderCustomizer != null) {
            builderCustomizer.accept(builder);
        }
        return builder.build();
    }

    private GenerateOptions buildGenerateOptions() {
        if (thinkingBudget == null || Boolean.FALSE.equals(thinkingEnabled)) {
            return null;
        }
        return GenerateOptions.builder().thinkingBudget(thinkingBudget).build();
    }

    private Boolean effectiveThinking(GenerateOptions options) {
        if (Boolean.FALSE.equals(thinkingEnabled)) {
            return Boolean.FALSE;
        }
        Integer budget = options == null ? null : options.getThinkingBudget();
        if (budget != null && budget <= 0) {
            throw new IllegalArgumentException("thinking budget must be positive");
        }
        return thinkingEnabled != null || budget != null ? Boolean.TRUE : null;
    }

    private GenerateOptions withoutThinkingBudget(GenerateOptions options) {
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
                .reasoningEffort(options.getReasoningEffort())
                .executionConfig(options.getExecutionConfig())
                .toolChoice(options.getToolChoice())
                .topK(options.getTopK())
                .seed(options.getSeed())
                .cacheControl(options.getCacheControl())
                .parallelToolCalls(options.getParallelToolCalls())
                .responseFormat(options.getResponseFormat())
                .additionalHeaders(options.getAdditionalHeaders())
                .additionalBodyParams(options.getAdditionalBodyParams())
                .additionalQueryParams(options.getAdditionalQueryParams())
                .build();
    }
}
