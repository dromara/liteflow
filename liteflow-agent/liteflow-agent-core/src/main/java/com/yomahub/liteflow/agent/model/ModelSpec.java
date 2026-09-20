package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Vendor-neutral 模型描述符。
 * <p>
 * 子类按平台命名（{@code OpenAISpec} / {@code AnthropicSpec} / 等），
 * 并暴露平台个性参数。共性参数（temperature、topP 等）在本基类提供。
 * <p>
 * {@link #resolve(AgentConfig)} 由各 provider 模块的子类实现：
 * 从 {@link AgentConfig} 取出 credential，把共性 + 个性参数翻译成
 * AgentScope 2 extension 的 {@code GenerateOptions}，并构造对应的 {@link Model}。
 *
 * @param <SELF> fluent self-type，便于子类链式调用保留具体类型
 */
public abstract class ModelSpec<SELF extends ModelSpec<SELF>> {

    private Integer contextWindow;
    private String modelCatalogProvider;

    /** Optional deployment limit for private models or endpoints absent from the catalog. */
    public SELF contextWindow(int tokens) {
        if (tokens <= 0) throw new IllegalArgumentException("contextWindow must be positive");
        this.contextWindow = tokens;
        return self();
    }

    /** Explicit models.dev provider identifier for a gateway whose limits match that provider. */
    public SELF modelCatalogProvider(String provider) {
        if (provider == null || provider.isBlank()) throw new IllegalArgumentException("provider must not be blank");
        this.modelCatalogProvider = provider;
        return self();
    }

    protected final Model recordMetadata(Model model, String provider, String baseUrl) {
        GenerateOptions options = mergeGenerateOptions(null);
        Integer output = options == null ? null : options.getMaxCompletionTokens();
        Boolean completion = output == null ? null : true;
        if (output == null && options != null && options.getMaxTokens() != null) {
            output = options.getMaxTokens();
            completion = false;
        }
        return com.yomahub.liteflow.agent.model.catalog.ModelMetadata.register(model,
                modelCatalogProvider != null ? modelCatalogProvider
                        : com.yomahub.liteflow.agent.model.catalog.ModelMetadata.providerFor(provider, baseUrl),
                contextWindow, output, completion);
    }

    private String apiKey;
    private String baseUrl;
    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Integer maxCompletionTokens;
    private Long seed;
    private Boolean stream;
    private Boolean cacheControl;
    private Boolean parallelToolCalls;
    private ExecutionConfig executionConfig;
    private Map<String, String> additionalHeaders;
    private Map<String, Object> additionalBodyParams;
    private Map<String, String> additionalQueryParams;
    private GenerateOptions generateOptions;

    @SuppressWarnings("unchecked")
    protected final SELF self() { return (SELF) this; }

    public SELF apiKey(String v)       { this.apiKey = v;       return self(); }
    public SELF baseUrl(String v)      { this.baseUrl = v;      return self(); }
    public SELF temperature(double v) { this.temperature = v; return self(); }
    public SELF topP(double v)        { this.topP = v;        return self(); }
    public SELF topK(int v)           { this.topK = v;        return self(); }
    public SELF maxTokens(int v)      { this.maxTokens = v;   return self(); }
    public SELF maxCompletionTokens(int v) {
        this.maxCompletionTokens = v;
        return self();
    }
    public SELF seed(long v)          { this.seed = v;        return self(); }
    public SELF stream(boolean v)     { this.stream = v;      return self(); }
    public SELF cacheControl(boolean v) { this.cacheControl = v; return self(); }
    public SELF parallelToolCalls(boolean v) {
        this.parallelToolCalls = v;
        return self();
    }
    public SELF executionConfig(ExecutionConfig v) {
        this.executionConfig = v;
        return self();
    }
    public SELF additionalHeader(String key, String value) {
        if (additionalHeaders == null) {
            additionalHeaders = new LinkedHashMap<>();
        }
        additionalHeaders.put(key, value);
        return self();
    }
    public SELF additionalHeaders(Map<String, String> values) {
        this.additionalHeaders = values == null ? null : new LinkedHashMap<>(values);
        return self();
    }
    public SELF additionalBodyParam(String key, Object value) {
        if (additionalBodyParams == null) {
            additionalBodyParams = new LinkedHashMap<>();
        }
        additionalBodyParams.put(key, value);
        return self();
    }
    public SELF additionalBodyParams(Map<String, Object> values) {
        this.additionalBodyParams = values == null ? null : new LinkedHashMap<>(values);
        return self();
    }
    public SELF additionalQueryParam(String key, String value) {
        if (additionalQueryParams == null) {
            additionalQueryParams = new LinkedHashMap<>();
        }
        additionalQueryParams.put(key, value);
        return self();
    }
    public SELF additionalQueryParams(Map<String, String> values) {
        this.additionalQueryParams = values == null ? null : new LinkedHashMap<>(values);
        return self();
    }
    public SELF generateOptions(GenerateOptions v) {
        this.generateOptions = v;
        return self();
    }

    public String getApiKey()          { return apiKey; }
    public String getBaseUrl()         { return baseUrl; }
    public Double getTemperature()   { return temperature; }
    public Double getTopP()          { return topP; }
    public Integer getTopK()         { return topK; }
    public Integer getMaxTokens()    { return maxTokens; }
    public Integer getMaxCompletionTokens() { return maxCompletionTokens; }
    public Long getSeed()            { return seed; }
    public Boolean getStream()       { return stream; }
    public Boolean getCacheControl() { return cacheControl; }
    public Boolean getParallelToolCalls() { return parallelToolCalls; }
    public ExecutionConfig getExecutionConfig() { return executionConfig; }
    public GenerateOptions getGenerateOptions() { return generateOptions; }

    protected final GenerateOptions mergeGenerateOptions(GenerateOptions providerOptions) {
        GenerateOptions providerWithCommon = GenerateOptions.mergeOptions(
                providerOptions, commonGenerateOptions());
        return GenerateOptions.mergeOptions(generateOptions, providerWithCommon);
    }

    private GenerateOptions commonGenerateOptions() {
        if (temperature == null
                && topP == null
                && topK == null
                && maxTokens == null
                && maxCompletionTokens == null
                && seed == null
                && stream == null
                && cacheControl == null
                && parallelToolCalls == null
                && executionConfig == null
                && isEmpty(additionalHeaders)
                && isEmpty(additionalBodyParams)
                && isEmpty(additionalQueryParams)) {
            return null;
        }

        GenerateOptions.Builder builder = GenerateOptions.builder()
                .temperature(temperature)
                .topP(topP)
                .topK(topK)
                .maxTokens(maxTokens)
                .maxCompletionTokens(maxCompletionTokens)
                .seed(seed)
                .stream(stream)
                .cacheControl(cacheControl)
                .parallelToolCalls(parallelToolCalls)
                .executionConfig(executionConfig);
        if (additionalHeaders != null) {
            builder.additionalHeaders(additionalHeaders);
        }
        if (additionalBodyParams != null) {
            builder.additionalBodyParams(additionalBodyParams);
        }
        if (additionalQueryParams != null) {
            builder.additionalQueryParams(additionalQueryParams);
        }
        return builder.build();
    }

    private static boolean isEmpty(Map<?, ?> values) {
        return values == null || values.isEmpty();
    }

    /**
     * 把本描述符解析为 agentscope {@link Model} 实例。
     * 实现需从 {@link AgentConfig} 中读取对应平台的 credential，
     * 并把共性 + 个性参数翻译成 AgentScope 2 的 GenerateOptions。
     * <p>
     * 本方法是框架 SPI：{@code HarnessAgentComponent} 在不同包中调用，
     * 因此必须为 {@code public}。
     */
    public abstract Model resolve(AgentConfig cfg);
}
