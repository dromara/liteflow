/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2021/3/18
 */
package com.yomahub.liteflow.property;

public class LiteflowConfig {

    private String ruleSource;

    private Integer slotSize;

    private Integer whenMaxWaitSecond;

    private Boolean enableLog;

    private Integer queueLimit;

    private Long delay;

    private Long period;

    public String getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(String ruleSource) {
        this.ruleSource = ruleSource;
    }

    public Integer getSlotSize() {
        return slotSize;
    }

    public void setSlotSize(Integer slotSize) {
        this.slotSize = slotSize;
    }

    public Integer getWhenMaxWaitSecond() {
        return whenMaxWaitSecond;
    }

    public void setWhenMaxWaitSecond(Integer whenMaxWaitSecond) {
        this.whenMaxWaitSecond = whenMaxWaitSecond;
    }

    public Integer getQueueLimit() {
        return queueLimit;
    }

    public void setQueueLimit(Integer queueLimit) {
        this.queueLimit = queueLimit;
    }

    public Long getDelay() {
        return delay;
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public Long getPeriod() {
        return period;
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

    public Boolean getEnableLog() {
        return enableLog;
    }

    public void setEnableLog(Boolean enableLog) {
        this.enableLog = enableLog;
    }
}
