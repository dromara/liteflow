package com.yomahub.liteflow.springboot;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiteflowProperty {

    @Value("${liteflow.ruleSource}")
    private String ruleSource;

    public String getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(String ruleSource) {
        this.ruleSource = ruleSource;
    }
}
