/**
 * <p>Title: liteflow</p>
 * <p>Description: 轻量级的组件式流程框架</p>
 *
 * @author Bryan.Zhang
 * @email weenyc31@163.com
 * @Date 2021/3/18
 */
package com.yomahub.liteflow.property;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

/**
 * liteflow的配置实体类
 * 这个类中的属性为什么不用基本类型，而用包装类型呢
 * 是因为这个类是springboot和spring的最终参数获取器，考虑到spring的场景，有些参数不是必须配置。基本类型就会出现默认值的情况。
 * 所以为了要有null值出现，这里采用包装类型
 *
 * @author Bryan.Zhang
 */
public class LiteflowConfig {

    /**
     * 是否启动liteflow自动装配
     */
    private Boolean enable;

    //流程定义资源地址
    private String ruleSource;

    //zk配置的node定义
    private String zkNode;

    //slot的数量
    private Integer slotSize;

    //并行线程执行器class路径
    private String threadExecutorClass;

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

    //这个属性为true，则支持多种不同的类型的配置
    //但是要注意，不能将主流程和子流程分配在不同类型配置文件中
    private Boolean supportMultipleType;

    //重试次数
    private Integer retryCount;
    // 节点执行器的类全名
    private String nodeExecutorClass;

    // requestId 生成器
    private String requestIdGeneratorClass;

    //是否打印liteflow banner
    private Boolean printBanner;

    //FlowExecutor的execute2Future的线程数
    private Integer mainExecutorWorks;

    //FlowExecutor的execute2Future的自定义线程池
    private String mainExecutorClass;

    //是否打印执行中的日志
    private Boolean printExecutionLog;

    //替补组件class路径
    private String substituteCmpClass;

    public Boolean getEnable() {
        if (ObjectUtil.isNull(enable)) {
            return Boolean.TRUE;
        } else {
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
        if (ObjectUtil.isNull(slotSize)) {
            return 1024;
        } else {
            return slotSize;
        }
    }

    public void setSlotSize(Integer slotSize) {
        this.slotSize = slotSize;
    }

    public Integer getWhenMaxWaitSeconds() {
        if (ObjectUtil.isNull(whenMaxWaitSeconds)) {
            return 15;
        } else {
            return whenMaxWaitSeconds;
        }
    }

    public void setWhenMaxWaitSeconds(Integer whenMaxWaitSeconds) {
        this.whenMaxWaitSeconds = whenMaxWaitSeconds;
    }

    public Integer getQueueLimit() {
        if (ObjectUtil.isNull(queueLimit)) {
            return 200;
        } else {
            return queueLimit;
        }
    }

    public void setQueueLimit(Integer queueLimit) {
        this.queueLimit = queueLimit;
    }

    public Long getDelay() {
        if (ObjectUtil.isNull(delay)) {
            return 300000L;
        } else {
            return delay;
        }
    }

    public void setDelay(Long delay) {
        this.delay = delay;
    }

    public Long getPeriod() {
        if (ObjectUtil.isNull(period)) {
            return 300000L;
        } else {
            return period;
        }
    }

    public void setPeriod(Long period) {
        this.period = period;
    }

    public Boolean getEnableLog() {
        if (ObjectUtil.isNull(enableLog)) {
            return Boolean.FALSE;
        } else {
            return enableLog;
        }
    }

    public void setEnableLog(Boolean enableLog) {
        this.enableLog = enableLog;
    }

    public Integer getWhenMaxWorkers() {
        if (ObjectUtil.isNull(whenMaxWorkers)) {
            return 16;
        } else {
            return whenMaxWorkers;
        }
    }

    public void setWhenMaxWorkers(Integer whenMaxWorkers) {
        this.whenMaxWorkers = whenMaxWorkers;
    }

    public Integer getWhenQueueLimit() {
        if (ObjectUtil.isNull(whenQueueLimit)) {
            return 512;
        } else {
            return whenQueueLimit;
        }
    }

    public void setWhenQueueLimit(Integer whenQueueLimit) {
        this.whenQueueLimit = whenQueueLimit;
    }

    public Boolean isParseOnStart() {
        if (ObjectUtil.isNull(parseOnStart)) {
            return Boolean.TRUE;
        } else {
            return parseOnStart;
        }
    }

    public void setParseOnStart(Boolean parseOnStart) {
        this.parseOnStart = parseOnStart;
    }

    public Boolean isSupportMultipleType() {
        if (ObjectUtil.isNull(supportMultipleType)) {
            return Boolean.FALSE;
        } else {
            return supportMultipleType;
        }
    }

    public void setSupportMultipleType(Boolean supportMultipleType) {
        this.supportMultipleType = supportMultipleType;
    }

    public Integer getRetryCount() {
        if (ObjectUtil.isNull(retryCount) || retryCount < 0) {
            return 0;
        } else {
            return retryCount;
        }
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getZkNode() {
        if (StrUtil.isBlank(zkNode)) {
            return "/lite-flow/flow";
        } else {
            return zkNode;
        }
    }

    public void setZkNode(String zkNode) {
        this.zkNode = zkNode;
    }

    public Boolean getPrintBanner() {
        if (ObjectUtil.isNull(printBanner)) {
            return Boolean.TRUE;
        } else {
            return printBanner;
        }
    }

    public void setPrintBanner(Boolean printBanner) {
        this.printBanner = printBanner;
    }

    public String getThreadExecutorClass() {
        if (StrUtil.isBlank(threadExecutorClass)){
            return "com.yomahub.liteflow.thread.LiteFlowDefaultWhenExecutorBuilder";
        }else{
            return threadExecutorClass;
        }
    }

    public void setThreadExecutorClass(String threadExecutorClass) {
        this.threadExecutorClass = threadExecutorClass;
    }

    public String getNodeExecutorClass() {
        if (StrUtil.isBlank(nodeExecutorClass)){
            return "com.yomahub.liteflow.flow.executor.DefaultNodeExecutor";
        }else{
            return nodeExecutorClass;
        }
    }

    public void setNodeExecutorClass(String nodeExecutorClass) {
        this.nodeExecutorClass = nodeExecutorClass;
    }

    public String getRequestIdGeneratorClass() {
        if(StrUtil.isBlank(this.requestIdGeneratorClass)){
            return "com.yomahub.liteflow.flow.id.DefaultRequestIdGenerator";
        }
        return requestIdGeneratorClass;
    }

    public void setRequestIdGeneratorClass(String requestIdGeneratorClass) {
        this.requestIdGeneratorClass = requestIdGeneratorClass;
    }

    public Integer getMainExecutorWorks() {
        if (ObjectUtil.isNull(mainExecutorWorks)){
            return 64;
        }else{
            return mainExecutorWorks;
        }
    }

    public void setMainExecutorWorks(Integer mainExecutorWorks) {
        this.mainExecutorWorks = mainExecutorWorks;
    }

    public String getMainExecutorClass() {
        if (StrUtil.isBlank(mainExecutorClass)){
            return "com.yomahub.liteflow.thread.LiteFlowDefaultMainExecutorBuilder";
        }else{
            return mainExecutorClass;
        }
    }

    public void setMainExecutorClass(String mainExecutorClass) {
        this.mainExecutorClass = mainExecutorClass;
    }

    public Boolean getPrintExecutionLog() {
        if (ObjectUtil.isNull(printExecutionLog)){
            return Boolean.TRUE;
        }else{
            return printExecutionLog;
        }
    }

    public void setPrintExecutionLog(Boolean printExecutionLog) {
        this.printExecutionLog = printExecutionLog;
    }

    public String getSubstituteCmpClass() {
        return substituteCmpClass;
    }

    public void setSubstituteCmpClass(String substituteCmpClass) {
        this.substituteCmpClass = substituteCmpClass;
    }
}
