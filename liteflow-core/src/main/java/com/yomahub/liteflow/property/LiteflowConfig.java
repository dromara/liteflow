/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2021/3/18
 */
package com.yomahub.liteflow.property;

/**
 * liteflow的配置实体类
 * @author Bryan.Zhang
 */
public class LiteflowConfig {

    //流程定义资源地址
    private String ruleSource;

    //slot的数量
    private Integer slotSize;

    //异步线程最大等待秒数
    private Integer whenMaxWaitSeconds;

    //是否打印监控log
    private Boolean enableLog;

    //监控存储信息最大队列数量
    private Integer queueLimit;

    //延迟多少秒打印
    private Long delay;

    //每隔多少秒打印
    private Long period;

    //异步线程池最大线程数
    private int whenMaxWorkers;

    //异步线程池最大队列数量
    private int whenQueueLimit;

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

    public Integer getWhenMaxWaitSeconds() {
        return whenMaxWaitSeconds;
    }

    public void setWhenMaxWaitSeconds(Integer whenMaxWaitSeconds) {
        this.whenMaxWaitSeconds = whenMaxWaitSeconds;
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

    public int getWhenMaxWorkers() {
        return whenMaxWorkers;
    }

    public void setWhenMaxWorkers(int whenMaxWorkers) {
        this.whenMaxWorkers = whenMaxWorkers;
    }

    public int getWhenQueueLimit() {
        return whenQueueLimit;
    }

    public void setWhenQueueLimit(int whenQueueLimit) {
        this.whenQueueLimit = whenQueueLimit;
    }
}
