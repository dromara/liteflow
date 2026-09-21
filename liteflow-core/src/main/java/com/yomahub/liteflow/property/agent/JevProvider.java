package com.yomahub.liteflow.property.agent;

/** Supported transports for Jev's typed decision protocol. */
public enum JevProvider {
    TYPESAFE("https://api.typesafe.ai/v1", "/systemone", "jev-1.13.0"),
    OPENROUTER("https://openrouter.ai/api/alpha", "/decisions", "typesafe/jev-1.13");

    private final String baseUrl;
    private final String path;
    private final String model;

    JevProvider(String baseUrl, String path, String model) {
        this.baseUrl = baseUrl;
        this.path = path;
        this.model = model;
    }

    public String defaultBaseUrl() { return baseUrl; }
    public String endpointPath() { return path; }
    public String defaultModel() { return model; }
}
