package com.yomahub.liteflow.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "liteflow", ignoreUnknownFields = true)
public class LiteflowProperty {

    private String ruleSource;

    public String getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(String ruleSource) {
        this.ruleSource = ruleSource;
    }
}
