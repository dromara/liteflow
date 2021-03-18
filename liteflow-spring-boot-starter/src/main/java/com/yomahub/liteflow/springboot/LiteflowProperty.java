package com.yomahub.liteflow.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "liteflow", ignoreUnknownFields = true)
public class LiteflowProperty {

    private String ruleSource;

    private int slotSize;

    private int whenMaxWaitSecond;

    public String getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(String ruleSource) {
        this.ruleSource = ruleSource;
    }

    public int getSlotSize() {
        return slotSize;
    }

    public void setSlotSize(int slotSize) {
        this.slotSize = slotSize;
    }

    public int getWhenMaxWaitSecond() {
        return whenMaxWaitSecond;
    }

    public void setWhenMaxWaitSecond(int whenMaxWaitSecond) {
        this.whenMaxWaitSecond = whenMaxWaitSecond;
    }
}
