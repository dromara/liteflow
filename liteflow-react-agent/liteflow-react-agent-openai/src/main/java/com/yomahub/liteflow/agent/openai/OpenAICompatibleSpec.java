package com.yomahub.liteflow.agent.openai;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.CredentialResolver;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.model.Model;

/**
 * OpenAI 兼容族 spec。与 {@link OpenAISpec} 共享所有可调参数，
 * 但 credential 来源换成 {@code liteflow.agent.openai-compatible.<configKey>}。
 * 子类内置默认 baseUrl，用户配置中的 baseUrl 优先生效。
 */
public class OpenAICompatibleSpec extends OpenAISpec {

    private final String configKey;
    private final String defaultBaseUrl;

    public OpenAICompatibleSpec(String configKey, String modelName, String defaultBaseUrl) {
        super(modelName);
        this.configKey = configKey;
        this.defaultBaseUrl = defaultBaseUrl;
    }

    @Override
    public Model resolve(AgentConfig cfg) {
        CredentialResolver.ResolvedCredential credential =
                CredentialResolver.resolveCompatible(
                        cfg.getOpenaiCompatible(),
                        configKey,
                        "liteflow.agent.openai-compatible",
                        getApiKey(),
                        getBaseUrl());
        String effectiveBaseUrl = credential.baseUrl();
        if ((effectiveBaseUrl == null || effectiveBaseUrl.isBlank())
                && defaultBaseUrl != null && !defaultBaseUrl.isBlank()) {
            effectiveBaseUrl = defaultBaseUrl;
        }
        if (effectiveBaseUrl == null || effectiveBaseUrl.isBlank()) {
            throw new AgentConfigException(
                    "Missing base URL: please configure "
                            + "liteflow.agent.openai-compatible."
                            + configKey
                            + ".base-url");
        }
        return buildModel(credential.apiKey(), effectiveBaseUrl);
    }
}
