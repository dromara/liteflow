package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.formatter.Formatter;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.openai.OpenAIChatModel;
import io.agentscope.extensions.model.openai.OpenAIClient;
import io.agentscope.extensions.model.openai.dto.OpenAIMessage;
import io.agentscope.extensions.model.openai.dto.OpenAIRequest;
import io.agentscope.extensions.model.openai.dto.OpenAIResponse;

import java.util.function.Consumer;

/**
 * OpenAI 系（含 OpenAI 兼容族）通用 spec。
 * 暴露 OpenAI 平台特有的 reasoningEffort / frequencyPenalty / presencePenalty 等参数。
 */
public class OpenAISpec extends ModelSpec<OpenAISpec> {

    private final String modelName;
    private String reasoningEffort;
    private Double frequencyPenalty;
    private Double presencePenalty;
    private String endpointPath;
    private Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter;
    private Boolean nativeStructuredOutput;
    private Boolean nativeStructuredOutputWithTools;
    private Consumer<OpenAIChatModel.Builder> builderCustomizer;

    public OpenAISpec(String modelName) {
        this.modelName = modelName;
    }

    public OpenAISpec reasoningEffort(String level) { this.reasoningEffort = level; return this; }
    public OpenAISpec frequencyPenalty(double v)    { this.frequencyPenalty = v;   return this; }
    public OpenAISpec presencePenalty(double v)     { this.presencePenalty = v;    return this; }
    public OpenAISpec endpointPath(String endpointPath) {
        this.endpointPath = endpointPath;
        return this;
    }
    public OpenAISpec formatter(
            Formatter<OpenAIMessage, OpenAIResponse, OpenAIRequest> formatter) {
        this.formatter = formatter;
        return this;
    }
    public OpenAISpec nativeStructuredOutput(boolean enabled) {
        this.nativeStructuredOutput = enabled;
        return this;
    }
    public OpenAISpec nativeStructuredOutputWithTools(boolean enabled) {
        this.nativeStructuredOutputWithTools = enabled;
        return this;
    }
    public OpenAISpec customizeBuilder(Consumer<OpenAIChatModel.Builder> customizer) {
        this.builderCustomizer = customizer;
        return this;
    }

    public String getModelName()         { return modelName; }
    public String getReasoningEffort()   { return reasoningEffort; }
    public Double getFrequencyPenalty()  { return frequencyPenalty; }
    public Double getPresencePenalty()   { return presencePenalty; }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential =
                CredentialResolver.resolveFirstClass(
                        cfg.getOpenai(),
                        "liteflow.agent.openai",
                        getApiKey(),
                        getBaseUrl());
        return buildModel(credential.apiKey(), credential.baseUrl());
    }

    /** 子类（OpenAICompatibleSpec）可覆盖以提供不同 baseUrl / apiKey 来源。 */
    protected Model buildModel(String apiKey, String baseUrl) {
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl != null && !baseUrl.isBlank()
                        ? baseUrl
                        : OpenAIClient.DEFAULT_BASE_URL_WITH_VERSION)
                .modelName(modelName);
        if (endpointPath != null && !endpointPath.isBlank()) {
            builder.endpointPath(endpointPath);
        }
        GenerateOptions options = mergeGenerateOptions(buildGenerateOptions());
        if (options != null) {
            builder.generateOptions(options);
            if (options.getStream() != null) {
                builder.stream(options.getStream());
            }
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

    /** 把 OpenAI 个性参数装配成 GenerateOptions；全部为 null 时返回 null。 */
    protected GenerateOptions buildGenerateOptions() {
        if (reasoningEffort == null
                && frequencyPenalty == null && presencePenalty == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (reasoningEffort != null)   b.reasoningEffort(reasoningEffort);
        if (frequencyPenalty != null)  b.frequencyPenalty(frequencyPenalty);
        if (presencePenalty != null)   b.presencePenalty(presencePenalty);
        return b.build();
    }
}
