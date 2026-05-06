package com.yomahub.liteflow.property.agent;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 大模型平台凭证对象。
 *
 * <p>头等平台（OpenAI / Anthropic / Gemini / DashScope）每个对应 {@link AgentConfig}
 * 中的一个 {@link PlatformCredential} 字段；OpenAI / Anthropic 兼容平台则以
 * {@code Map<String, PlatformCredential>} 形式按用户自定义 key 存放。
 *
 * <p>本对象在各 {@code ModelSpec.resolve()} 中被读取，用于构造对应的
 * AgentScope 模型实例（如 {@code OpenAIChatModel}、{@code AnthropicChatModel}）。
 * 实际的非空校验与缺失提示统一由 {@code CredentialResolver} 完成。
 */
public class PlatformCredential {

    /**
     * 平台 API Key（必填）。
     *
     * <p>{@code CredentialResolver} 在解析时若为空将抛出 {@code AgentConfigException}，
     * 并提示用户检查对应配置路径下的 {@code api-key} 项。
     */
    private String apiKey;

    /**
     * 平台基础地址，可选。
     *
     * <p>留空时使用 SDK 默认地址；非空时会传给对应模型 Builder 的 {@code baseUrl(...)}，
     * 用于自建网关、代理或私有化部署等场景。
     */
    private String baseUrl;

    /**
     * 预留的扩展参数 Map，对应配置项的 {@code extra.*}。
     *
     * <p>当前 LiteFlow 内置的 ProviderSpec 尚未读取该字段，主要用于自定义扩展
     * （例如未来新增的鉴权头、自定义业务标识等），保留以保持配置结构的向前兼容。
     */
    private Map<String, String> extra = new LinkedHashMap<>();

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Map<String, String> getExtra() {
        return extra;
    }

    public void setExtra(Map<String, String> extra) {
        this.extra = extra;
    }
}
