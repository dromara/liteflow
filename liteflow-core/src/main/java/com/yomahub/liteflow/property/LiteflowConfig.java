/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2021/3/18
 */
package com.yomahub.liteflow.property;

import cn.hutool.core.util.ObjectUtil;

/**
 * liteflow的配置实体类
 * 这个类中的属性为什么不用基本类型，而用包装类型呢
 * 是因为这个类是springboot和spring的最终参数获取器，考虑到spring的场景，有些参数不是必须配置。基本类型就会出现默认值的情况。
 * 所以为了要有null值出现，这里采用包装类型
 * @author Bryan.Zhang
 */
public class LiteflowConfig {

    /**
     * 是否启动liteflow自动装配
     */
    private Boolean enable;

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
    private Integer whenMaxWorkers;

    //异步线程池最大队列数量
    private Integer whenQueueLimit;

    //是否在启动时解析规则文件
    //这个参数主要给编码式注册元数据的场景用的，结合FlowBus.addNode一起用
    private Boolean parseOnStart;

    public Boolean getEnable() {
        if (ObjectUtil.isNull(enable)){
            return true;
        }else{
            return enable;
        }
    }

    public void setEnable(Boolean enable) {
        this.enable = enable;
    }

    public String getRuleSource() {
        return ruleSource;
    }

    public void setRuleSource(String ruleSource) {
        this.ruleSource = ruleSource;
    }

    public Integer getSlotSize() {
        if (ObjectUtil.isNull(slotSize)){
            return 1024;
        }else{
            return slotSize;
        }
    }

    public void setSlotSize(Integer slotSize) {
        this.slotSize = slotSize;
    }

    public Integer getWhenMaxWaitSeconds() {
        if (ObjectUtil.isNull(whenMaxWaitSeconds)){
            return 15;
        }else{
            return whenMaxWaitSeconds;
        }
    }

    public void setWhenMaxWaitSeconds(Integer whenMaxWaitSeconds) {
        this.whenMaxWaitSeconds = whenMaxWaitSeconds;
    }

    public Integer getQueueLimit() {
        if (ObjectUtil.isNull(queueLimit)){
            return 200;
        }else{
            return queueLimit;
        }
    }

    public void setQueueLimit(Integer queueLimit) {
        this.queueLimit = queueLimit;
    }

    public Long getDelay() {
        if (ObjectUtil.isNull(delay)){
            return 300000L;
        }else{
            return delay;
        }
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public Long getPeriod() {
        if (ObjectUtil.isNull(period)){
            return 300000L;
        }else{
            return period;
        }
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

    public Boolean getEnableLog() {
        if (ObjectUtil.isNull(enableLog)){
            return false;
        }else{
            return enableLog;
        }
    }

    public void setEnableLog(Boolean enableLog) {
        this.enableLog = enableLog;
    }

    public Integer getWhenMaxWorkers() {
        if (ObjectUtil.isNull(whenMaxWorkers)){
            return Runtime.getRuntime().availableProcessors() * 2;
        }else{
            return whenMaxWorkers;
        }
    }

    public void setWhenMaxWorkers(Integer whenMaxWorkers) {
        this.whenMaxWorkers = whenMaxWorkers;
    }

    public Integer getWhenQueueLimit() {
        if (ObjectUtil.isNull(whenQueueLimit)){
            return 100;
        }else{
            return whenQueueLimit;
        }
    }

    public void setWhenQueueLimit(Integer whenQueueLimit) {
        this.whenQueueLimit = whenQueueLimit;
    }

    public Boolean isParseOnStart() {
        if (ObjectUtil.isNull(parseOnStart)){
            return true;
        }else{
            return parseOnStart;
        }
    }

    public void setParseOnStart(Boolean parseOnStart) {
        this.parseOnStart = parseOnStart;
    }
}
