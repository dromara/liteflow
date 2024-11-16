package com.yomahub.liteflow.springboot.config;

import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.springboot.LiteflowMonitorProperty;
import com.yomahub.liteflow.springboot.LiteflowProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * LiteflowConfig的装配类 这个装配类主要是把监控器的配置参数类和流程配置参数类作一个合并，转换成统一的配置参数类。
 * 同时这里设置了默认的参数路径，如果在springboot的application.properties/yml里没取到的话，就取默认值
 *
 * @author Bryan.Zhang
 * @author jason
 */
@Configuration
@EnableConfigurationProperties({ LiteflowProperty.class, LiteflowMonitorProperty.class })
@PropertySource(name = "Liteflow Default Properties", value = "classpath:/META-INF/liteflow-default.properties")
public class LiteflowPropertyAutoConfiguration {

	@Bean
	public LiteflowConfig liteflowConfig(LiteflowProperty property, LiteflowMonitorProperty liteflowMonitorProperty) {
		LiteflowConfig liteflowConfig = new LiteflowConfig();
		liteflowConfig.setRuleSource(property.getRuleSource());
		liteflowConfig.setRuleSourceExtData(property.getRuleSourceExtData());
		liteflowConfig.setRuleSourceExtDataMap(property.getRuleSourceExtDataMap());
		liteflowConfig.setSlotSize(property.getSlotSize());
		liteflowConfig.setWhenMaxWaitSeconds(property.getWhenMaxWaitSeconds());
		liteflowConfig.setWhenMaxWaitTime(property.getWhenMaxWaitTime());
		liteflowConfig.setWhenMaxWaitTimeUnit(property.getWhenMaxWaitTimeUnit());
		liteflowConfig.setWhenThreadPoolIsolate(property.isWhenThreadPoolIsolate());
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
		liteflowConfig.setEnableMonitorFile(property.isEnableMonitorFile());
		liteflowConfig.setFallbackCmpEnable(property.isFallbackCmpEnable());
		liteflowConfig.setFastLoad(property.isFastLoad());
		liteflowConfig.setEnableLog(liteflowMonitorProperty.isEnableLog());
		liteflowConfig.setQueueLimit(liteflowMonitorProperty.getQueueLimit());
		liteflowConfig.setDelay(liteflowMonitorProperty.getDelay());
		liteflowConfig.setPeriod(liteflowMonitorProperty.getPeriod());
		liteflowConfig.setScriptSetting(property.getScriptSetting());
        liteflowConfig.setGlobalThreadPoolExecutorClass(property.getGlobalThreadPoolExecutorClass());
        liteflowConfig.setGlobalThreadPoolQueueSize(property.getGlobalThreadPoolQueueSize());
        liteflowConfig.setGlobalThreadPoolSize(property.getGlobalThreadPoolSize());
		liteflowConfig.setEnableNodeInstanceId(property.isEnableNodeInstanceId());
		return liteflowConfig;
	}

}
