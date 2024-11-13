package com.yomahub.liteflow.solon.config;

import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.noear.solon.annotation.Bean;
import org.noear.solon.annotation.Configuration;
import org.noear.solon.annotation.Inject;

/**
 * LiteflowConfig的装配类
 *
 * 这个装配类主要是把监控器的配置参数类和流程配置参数类作一个合并，转换成统一的配置参数类。 同时这里设置了默认的参数路径，如果在 solon 的
 * app.properties/yml 里没取到的话，就取默认值
 *
 * @author Bryan.Zhang
 * @author noear
 * @author jason
 * @since 2.9
 */
@Configuration
public class LiteflowAutoConfiguration {

	@Inject(value = "${liteflow.monitor.enableLog}", required = false)
	boolean enableLog;

	@Bean
	public LiteflowConfig liteflowConfig(LiteflowProperty property, LiteflowMonitorProperty liteflowMonitorProperty) {
		LiteflowConfig liteflowConfig = new LiteflowConfig();
		liteflowConfig.setRuleSource(property.getRuleSource());
		liteflowConfig.setRuleSourceExtData(property.getRuleSourceExtData());
		liteflowConfig.setRuleSourceExtDataMap(property.getRuleSourceExtDataMap());
		liteflowConfig.setSlotSize(property.getSlotSize());
		liteflowConfig.setWhenMaxWaitSeconds(property.getWhenMaxWaitSeconds());
		liteflowConfig.setEnableLog(liteflowMonitorProperty.isEnableLog());
		liteflowConfig.setQueueLimit(liteflowMonitorProperty.getQueueLimit());
		liteflowConfig.setDelay(liteflowMonitorProperty.getDelay());
		liteflowConfig.setPeriod(liteflowMonitorProperty.getPeriod());
		liteflowConfig.setParseMode(property.getParseMode());
		liteflowConfig.setEnable(property.isEnable());
		liteflowConfig.setSupportMultipleType(property.isSupportMultipleType());
		liteflowConfig.setRetryCount(property.getRetryCount());
		liteflowConfig.setPrintBanner(property.isPrintBanner());
		liteflowConfig.setNodeExecutorClass(property.getNodeExecutorClass());
		liteflowConfig.setRequestIdGeneratorClass(property.getRequestIdGeneratorClass());
		liteflowConfig.setMainExecutorWorks(property.getMainExecutorWorks());
		liteflowConfig.setMainExecutorClass(property.getMainExecutorClass());
		liteflowConfig.setPrintExecutionLog(property.isPrintExecutionLog());
		liteflowConfig.setFallbackCmpEnable(property.isFallbackCmpEnable());
        liteflowConfig.setGlobalThreadPoolExecutorClass(property.getGlobalThreadPoolExecutorClass());
        liteflowConfig.setGlobalThreadPoolSize(property.getGlobalThreadPoolSize());
        liteflowConfig.setGlobalThreadPoolQueueSize(property.getGlobalThreadPoolQueueSize());
        liteflowConfig.setWhenThreadPoolIsolate(property.getWhenThreadPoolIsolate());
		liteflowConfig.setEnableNodeInstanceId(property.isEnableNodeInstanceId());
		return liteflowConfig;
	}

	@Bean
	public MonitorBus monitorBus(LiteflowConfig liteflowConfig) {
		if (enableLog) {
			return new MonitorBus(liteflowConfig);
		}
		else {
			return null; // null 即是没创建
		}
	}

}
