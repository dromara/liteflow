package com.yomahub.liteflow.springboot;

import cn.hutool.core.util.StrUtil;
import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spring.ComponentScaner;
import com.yomahub.liteflow.util.SpringAware;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

import javax.swing.*;
import java.util.List;

@Configuration
@EnableConfigurationProperties({LiteflowProperty.class,LiteflowMonitorProperty.class})
@ConditionalOnProperty(prefix = "liteflow", name = "rule-source")
@PropertySource(
        name = "liteflow Default Properties",
        value = "classpath:/META-INF/liteflow-default.properties")
public class LiteflowAutoConfiguration {

    @Bean
    public LiteflowConfig liteflowConfig(LiteflowProperty property, LiteflowMonitorProperty liteflowMonitorProperty){
        LiteflowConfig liteflowConfig = new LiteflowConfig();
        liteflowConfig.setRuleSource(property.getRuleSource());
        liteflowConfig.setSlotSize(property.getSlotSize());
        liteflowConfig.setWhenMaxWaitSecond(property.getWhenMaxWaitSecond());
        liteflowConfig.setEnableLog(liteflowMonitorProperty.isEnableLog());
        liteflowConfig.setQueueLimit(liteflowMonitorProperty.getQueueLimit());
        liteflowConfig.setDelay(liteflowMonitorProperty.getDelay());
        liteflowConfig.setPeriod(liteflowMonitorProperty.getPeriod());
        return liteflowConfig;
    }

    @Bean
    public ComponentScaner componentScaner(LiteflowConfig liteflowConfig){
        return new ComponentScaner();
    }

    @Bean
    public FlowExecutor flowExecutor(LiteflowConfig liteflowConfig){
        if(StrUtil.isNotBlank(liteflowConfig.getRuleSource())){
            FlowExecutor flowExecutor = new FlowExecutor();
            flowExecutor.setLiteflowConfig(liteflowConfig);
            return flowExecutor;
        }else{
            return null;
        }
    }

    @Bean
    public SpringAware springAware(){
        return new SpringAware();
    }

    @Bean
    public LiteflowExecutorInit liteflowExecutorInit(FlowExecutor flowExecutor, SpringAware springAware){
        return new LiteflowExecutorInit(flowExecutor);
    }

    @Bean
    public MonitorBus monitorBus(LiteflowConfig liteflowConfig){
        return new MonitorBus(liteflowConfig);
    }
}
