package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.PlatformCredential;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.OpenAIChatModel;

/**
 * OpenAI 系（含 OpenAI 兼容族）通用 spec。
 * 暴露 OpenAI 平台特有的 reasoningEffort / frequencyPenalty / presencePenalty 等参数。
 */
public class OpenAISpec extends ModelSpec<OpenAISpec> {

    private final String modelName;
    private String reasoningEffort;
    private Double frequencyPenalty;
    private Double presencePenalty;

    public OpenAISpec(String modelName) {
        this.modelName = modelName;
    }

    public OpenAISpec reasoningEffort(String level) { this.reasoningEffort = level; return this; }
    public OpenAISpec frequencyPenalty(double v)    { this.frequencyPenalty = v;   return this; }
    public OpenAISpec presencePenalty(double v)     { this.presencePenalty = v;    return this; }

    public String getModelName()         { return modelName; }
    public String getReasoningEffort()   { return reasoningEffort; }
    public Double getFrequencyPenalty()  { return frequencyPenalty; }
    public Double getPresencePenalty()   { return presencePenalty; }

    @Override
    public Model resolve(AgentConfig cfg) {
        PlatformCredential cred = CredentialResolver.requireFirstClass(
                cfg.getOpenai(), "liteflow.agent.openai");
        return buildModel(cred.getApiKey(), cred.getBaseUrl());
    }

    /** 子类（OpenAICompatibleSpec）可覆盖以提供不同 baseUrl / apiKey 来源。 */
    protected Model buildModel(String apiKey, String baseUrl) {
        OpenAIChatModel.Builder builder = OpenAIChatModel.builder()
                .apiKey(apiKey)
                .modelName(modelName);
        if (baseUrl != null && !baseUrl.isBlank()) {
            builder.baseUrl(baseUrl);
        }
        GenerateOptions options = buildGenerateOptions();
        if (options != null) {
            builder.generateOptions(options);
        }
        if (getStream() != null) {
            builder.stream(getStream());
        }
        return builder.build();
    }

    /** 把共性 + 个性参数装配成 GenerateOptions；全部为 null 时返回 null。 */
    protected GenerateOptions buildGenerateOptions() {
        if (getTemperature() == null && getTopP() == null && getTopK() == null
                && getMaxTokens() == null && getSeed() == null
                && getCacheControl() == null
                && reasoningEffort == null
                && frequencyPenalty == null && presencePenalty == null) {
            return null;
        }
        GenerateOptions.Builder b = GenerateOptions.builder();
        if (getTemperature() != null)  b.temperature(getTemperature());
        if (getTopP() != null)         b.topP(getTopP());
        if (getTopK() != null)         b.topK(getTopK());
        if (getMaxTokens() != null)    b.maxTokens(getMaxTokens());
        if (getSeed() != null)         b.seed(getSeed());
        if (getCacheControl() != null) b.cacheControl(getCacheControl());
        if (reasoningEffort != null)   b.reasoningEffort(reasoningEffort);
        if (frequencyPenalty != null)  b.frequencyPenalty(frequencyPenalty);
        if (presencePenalty != null)   b.presencePenalty(presencePenalty);
        return b.build();
    }
}
