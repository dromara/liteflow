package com.yomahub.liteflow.property.agent;

import java.time.Duration;

/** Connection and routing defaults for {@code liteflow.agent.jev.*}, validated when used. */
public class JevConfig {

    private JevProvider provider = JevProvider.TYPESAFE;
    private String apiKey;
    /** Optional API root override; null or blank uses the selected provider's default. */
    private String baseUrl;
    /** Optional model override; null or blank uses the selected provider's default. */
    private String model;
    /** Maximum wait for the complete HTTP response, including its body. */
    private Duration timeout = Duration.ofSeconds(3);
    /** Application-specific threshold; calibrate with representative inputs before production use. */
    private double minConfidence = 0.6;

    public JevProvider getProvider() {
        if (provider == null) {
            throw new IllegalArgumentException("liteflow.agent.jev.provider must be typesafe or openrouter");
        }
        return provider;
    }
    public void setProvider(JevProvider provider) { this.provider = provider; }
    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() {
        return baseUrl == null || baseUrl.trim().isEmpty() ? getProvider().defaultBaseUrl() : baseUrl;
    }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getModel() {
        return model == null || model.trim().isEmpty() ? getProvider().defaultModel() : model;
    }
    public void setModel(String model) { this.model = model; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration timeout) { this.timeout = timeout; }
    public double getMinConfidence() { return minConfidence; }
    public void setMinConfidence(double minConfidence) { this.minConfidence = minConfidence; }
}
