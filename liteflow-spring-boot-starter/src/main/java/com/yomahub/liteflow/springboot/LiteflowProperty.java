package com.yomahub.liteflow.springboot;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.enums.ParseModeEnum;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 执行流程主要的参数类
 *
 * @author Bryan.Zhang
 * @author jason
 */
@ConfigurationProperties(prefix = "liteflow", ignoreUnknownFields = true)
public class LiteflowProperty {

	// 是否装配liteflow
	private boolean enable;

	// 流程定义资源地址
	private String ruleSource;

	// 流程资源扩展数据，String格式
	private String ruleSourceExtData;

	// 流程资源扩展数据，Map格式
	private Map<String, String> ruleSourceExtDataMap;

	// slot的数量
	private int slotSize;

	// FlowExecutor的execute2Future的线程数
	private int mainExecutorWorks;

	// FlowExecutor的execute2Future的自定义线程池
	private String mainExecutorClass;

	// 异步线程最大等待描述
	@Deprecated
	private int whenMaxWaitSeconds;

	private int whenMaxWaitTime;

	private TimeUnit whenMaxWaitTimeUnit;

	// 异步线程池是否隔离
	private boolean whenThreadPoolIsolate;

	// 解析模式，一共有三种，具体看其定义
	private ParseModeEnum parseMode;

	// 这个属性为true，则支持多种不同的类型的配置
	// 但是要注意，不能将主流程和子流程分配在不同类型配置文件中
	private boolean supportMultipleType;

	// 重试次数
	@Deprecated
	private int retryCount;

	// 是否打印liteflow banner
	private boolean printBanner;

	// 节点执行器class全名
	private String nodeExecutorClass;

	// requestId 生成器
	private String requestIdGeneratorClass;

	// 是否打印执行过程中的日志
	private boolean printExecutionLog;

	// 规则文件/脚本文件变更监听
	private boolean enableMonitorFile;
	
	// 是否启用组件降级
	private boolean fallbackCmpEnable;

	//是否快速加载规则，如果快速加载规则意味着不用copyOnWrite机制了
	private boolean fastLoad;

	//是否检查节点存在
	private boolean checkNodeExists;

	//脚本特殊设置选项
	private Map<String, String> scriptSetting;

    //全局线程池所用class路径(when+异步循环)
    private String globalThreadPoolExecutorClass;

    //全局线程池最大线程数(when+异步循环)
    private Integer globalThreadPoolSize;

    //全局线程池最大队列数(when+异步循环)
    private Integer globalThreadPoolQueueSize;

	//是否启用节点实例ID
	private boolean enableNodeInstanceId;

	public boolean isEnableMonitorFile() {
		return enableMonitorFile;
	}

	public void setEnableMonitorFile(boolean enableMonitorFile) {
		this.enableMonitorFile = enableMonitorFile;
	}

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

	@Deprecated
	public int getWhenMaxWaitSeconds() {
		return whenMaxWaitSeconds;
	}

	@Deprecated
	public void setWhenMaxWaitSeconds(int whenMaxWaitSeconds) {
		this.whenMaxWaitSeconds = whenMaxWaitSeconds;
	}

	public ParseModeEnum getParseMode() {
		return parseMode;
	}

	public void setParseMode(ParseModeEnum parseMode) {
		this.parseMode = parseMode;
	}

	public boolean isSupportMultipleType() {
		return supportMultipleType;
	}

	public void setSupportMultipleType(boolean supportMultipleType) {
		this.supportMultipleType = supportMultipleType;
	}

	@Deprecated
	public int getRetryCount() {
		return retryCount;
	}

	@Deprecated
	public void setRetryCount(int retryCount) {
		this.retryCount = retryCount;
	}

	public boolean isPrintBanner() {
		return printBanner;
	}

	public void setPrintBanner(boolean printBanner) {
		this.printBanner = printBanner;
	}

	public String getNodeExecutorClass() {
		return nodeExecutorClass;
	}

	public void setNodeExecutorClass(String nodeExecutorClass) {
		this.nodeExecutorClass = nodeExecutorClass;
	}

	public int getMainExecutorWorks() {
		return mainExecutorWorks;
	}

	public void setMainExecutorWorks(int mainExecutorWorks) {
		this.mainExecutorWorks = mainExecutorWorks;
	}

	public String getMainExecutorClass() {
		return mainExecutorClass;
	}

	public void setMainExecutorClass(String mainExecutorClass) {
		this.mainExecutorClass = mainExecutorClass;
	}

	public boolean isPrintExecutionLog() {
		return printExecutionLog;
	}

	public void setPrintExecutionLog(boolean printExecutionLog) {
		this.printExecutionLog = printExecutionLog;
	}

	public String getRequestIdGeneratorClass() {
		return requestIdGeneratorClass;
	}

	public void setRequestIdGeneratorClass(String requestIdGeneratorClass) {
		this.requestIdGeneratorClass = requestIdGeneratorClass;
	}

	public String getRuleSourceExtData() {
		return ruleSourceExtData;
	}

	public void setRuleSourceExtData(String ruleSourceExtData) {
		this.ruleSourceExtData = ruleSourceExtData;
	}

	public Map<String, String> getRuleSourceExtDataMap() {
		return ruleSourceExtDataMap;
	}

	public void setRuleSourceExtDataMap(Map<String, String> ruleSourceExtDataMap) {
		this.ruleSourceExtDataMap = ruleSourceExtDataMap;
	}

	public int getWhenMaxWaitTime() {
		return whenMaxWaitTime;
	}

	public void setWhenMaxWaitTime(int whenMaxWaitTime) {
		this.whenMaxWaitTime = whenMaxWaitTime;
	}

	public TimeUnit getWhenMaxWaitTimeUnit() {
		return whenMaxWaitTimeUnit;
	}

	public void setWhenMaxWaitTimeUnit(TimeUnit whenMaxWaitTimeUnit) {
		this.whenMaxWaitTimeUnit = whenMaxWaitTimeUnit;
	}

	public boolean isFallbackCmpEnable() {
		return fallbackCmpEnable;
	}

	public void setFallbackCmpEnable(boolean fallbackCmpEnable) {
		this.fallbackCmpEnable = fallbackCmpEnable;
	}

	public boolean isWhenThreadPoolIsolate() {
		return whenThreadPoolIsolate;
	}

	public void setWhenThreadPoolIsolate(boolean whenThreadPoolIsolate) {
		this.whenThreadPoolIsolate = whenThreadPoolIsolate;
	}

	public boolean isFastLoad() {
		return fastLoad;
	}

	public void setFastLoad(boolean fastLoad) {
		this.fastLoad = fastLoad;
	}

	public boolean isCheckNodeExists() {
		return checkNodeExists;
	}

	public void setCheckNodeExists(boolean checkNodeExists) {
		this.checkNodeExists = checkNodeExists;
	}

	public Map<String, String> getScriptSetting() {
		return scriptSetting;
	}

	public void setScriptSetting(Map<String, String> scriptSetting) {
		this.scriptSetting = scriptSetting;
	}

    public Integer getGlobalThreadPoolSize() {
        if (ObjectUtil.isNull(globalThreadPoolSize)) {
            return 16;
        } else {
            return globalThreadPoolSize;
        }
    }

    public void setGlobalThreadPoolSize(Integer globalThreadPoolSize) {
        this.globalThreadPoolSize = globalThreadPoolSize;
    }

    public Integer getGlobalThreadPoolQueueSize() {
        if (ObjectUtil.isNull(globalThreadPoolQueueSize)) {
            return 512;
        } else {
            return globalThreadPoolQueueSize;
        }
    }

    public void setGlobalThreadPoolQueueSize(Integer globalThreadPoolQueueSize) {
        this.globalThreadPoolQueueSize = globalThreadPoolQueueSize;
    }

    public String getGlobalThreadPoolExecutorClass() {
        if (StrUtil.isBlank(globalThreadPoolExecutorClass)) {
            return "com.yomahub.liteflow.thread.LiteFlowDefaultGlobalExecutorBuilder";
        } else {
            return globalThreadPoolExecutorClass;
        }
    }

    public void setGlobalThreadPoolExecutorClass(String globalThreadPoolExecutorClass) {
        this.globalThreadPoolExecutorClass = globalThreadPoolExecutorClass;
    }

	public boolean isEnableNodeInstanceId() {
		return enableNodeInstanceId;
	}

	public void setEnableNodeInstanceId(boolean enableNodeInstanceId) {
		this.enableNodeInstanceId = enableNodeInstanceId;
	}
}
