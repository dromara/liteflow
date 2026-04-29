package com.yomahub.liteflow.agent.model;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.PlatformCredential;

import java.util.Map;

/**
 * 帮助 provider 模块的 {@link ModelSpec#resolve} 实现统一地从
 * {@link com.yomahub.liteflow.property.agent.AgentConfig} 中取出
 * {@link PlatformCredential}，并在缺失时抛出带配置路径提示的
 * {@link AgentConfigException}。
 */
public final class CredentialResolver {

    private CredentialResolver() {}

    /**
     * 取头等平台 credential（如 openai / anthropic / gemini / dashscope）。
     *
     * @param cred       从 AgentConfig 中拿到的 credential 实例（可能为 null）
     * @param configPath 配置路径前缀，例如 "liteflow.agent.openai"，用于错误信息
     */
    public static PlatformCredential requireFirstClass(PlatformCredential cred, String configPath) {
        if (cred == null || isBlank(cred.getApiKey())) {
            throw new AgentConfigException(
                    "Missing API key: please configure " + configPath + ".api-key");
        }
        return cred;
    }

    /**
     * 取兼容 Map 中的 credential（如 openaiCompatible.deepseek）。
     *
     * @param map        兼容 Map（可能为 null 或缺 key）
     * @param key        平台 key，如 "deepseek"
     * @param configPath 配置路径前缀，例如 "liteflow.agent.openai-compatible"
     */
    public static PlatformCredential requireCompatible(
            Map<String, PlatformCredential> map, String key, String configPath) {
        PlatformCredential cred = (map == null) ? null : map.get(key);
        if (cred == null) {
            throw new AgentConfigException(
                    "Missing platform credential: please configure "
                            + configPath + "." + key + ".api-key");
        }
        if (isBlank(cred.getApiKey())) {
            throw new AgentConfigException(
                    "Missing API key: please configure "
                            + configPath + "." + key + ".api-key");
        }
        return cred;
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
