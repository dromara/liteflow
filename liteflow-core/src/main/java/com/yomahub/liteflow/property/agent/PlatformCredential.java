package com.yomahub.liteflow.property.agent;

import java.util.LinkedHashMap;
import java.util.Map;

public class PlatformCredential {
    private String apiKey;
    private String baseUrl;
    private Map<String, String> extra = new LinkedHashMap<>();

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public Map<String, String> getExtra() { return extra; }
    public void setExtra(Map<String, String> extra) { this.extra = extra; }
}
