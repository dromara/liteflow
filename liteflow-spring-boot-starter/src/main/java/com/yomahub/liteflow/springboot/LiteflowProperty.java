package com.yomahub.liteflow.springboot;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 执行流程主要的参数类
 * @author Bryan.Zhang
 */
@ConfigurationProperties(prefix = "liteflow", ignoreUnknownFields = true)
public class LiteflowProperty {

    //是否装配liteflow
    private boolean enable;

    //流程定义资源地址
    private String ruleSource;

    //slot的数量
    private int slotSize;

    //异步线程最大等待描述
    private int whenMaxWaitSeconds;

    //异步线程池最大线程数
    private int whenMaxWorkers;

    //异步线程池最大队列数量
    private int whenQueueLimit;

    //是否在启动时解析规则文件
    //这个参数主要给编码式注册元数据的场景用的，结合FlowBus.addNode一起用
    private boolean parseOnStart;

    //这个属性为true，则支持多种不同的类型的配置
    //但是要注意，不能将主流程和子流程分配在不同类型配置文件中
    private boolean supportMultipleType;

    //重试次数
    private int retryCount;

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

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

    public int getWhenMaxWaitSeconds() {
        return whenMaxWaitSeconds;
    }

    public void setWhenMaxWaitSeconds(int whenMaxWaitSeconds) {
        this.whenMaxWaitSeconds = whenMaxWaitSeconds;
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

    public boolean isParseOnStart() {
        return parseOnStart;
    }

    public void setParseOnStart(boolean parseOnStart) {
        this.parseOnStart = parseOnStart;
    }

    public boolean isSupportMultipleType() {
        return supportMultipleType;
    }

    public void setSupportMultipleType(boolean supportMultipleType) {
        this.supportMultipleType = supportMultipleType;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
