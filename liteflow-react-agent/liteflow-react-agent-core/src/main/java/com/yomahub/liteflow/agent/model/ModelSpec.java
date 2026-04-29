package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.Model;

/**
 * Vendor-neutral 模型描述符。
 * <p>
 * 子类按平台命名（{@code OpenAISpec} / {@code AnthropicSpec} / 等），
 * 并暴露平台个性参数。共性参数（temperature、topP 等）在本基类提供。
 * <p>
 * {@link #resolve(AgentConfig)} 由各 provider 模块的子类实现：
 * 从 {@link AgentConfig} 取出 credential，把共性 + 个性参数翻译成
 * agentscope 的 {@code GenerateOptions}，并构造对应的 {@link Model}。
 *
 * @param <SELF> fluent self-type，便于子类链式调用保留具体类型
 */
public abstract class ModelSpec<SELF extends ModelSpec<SELF>> {

    private Double temperature;
    private Double topP;
    private Integer topK;
    private Integer maxTokens;
    private Long seed;
    private Boolean stream;
    private Boolean cacheControl;

    @SuppressWarnings("unchecked")
    protected final SELF self() { return (SELF) this; }

    public SELF temperature(double v) { this.temperature = v; return self(); }
    public SELF topP(double v)        { this.topP = v;        return self(); }
    public SELF topK(int v)           { this.topK = v;        return self(); }
    public SELF maxTokens(int v)      { this.maxTokens = v;   return self(); }
    public SELF seed(long v)          { this.seed = v;        return self(); }
    public SELF stream(boolean v)     { this.stream = v;      return self(); }
    public SELF cacheControl(boolean v) { this.cacheControl = v; return self(); }

    public Double getTemperature()   { return temperature; }
    public Double getTopP()          { return topP; }
    public Integer getTopK()         { return topK; }
    public Integer getMaxTokens()    { return maxTokens; }
    public Long getSeed()            { return seed; }
    public Boolean getStream()       { return stream; }
    public Boolean getCacheControl() { return cacheControl; }

    /**
     * 把本描述符解析为 agentscope {@link Model} 实例。
     * 实现需从 {@link AgentConfig} 中读取对应平台的 credential，
     * 并把共性 + 个性参数翻译成 agentscope 的 GenerateOptions。
     */
    protected abstract Model resolve(AgentConfig cfg);
}
