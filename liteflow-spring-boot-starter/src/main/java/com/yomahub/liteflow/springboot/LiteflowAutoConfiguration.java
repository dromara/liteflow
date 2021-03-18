package com.yomahub.liteflow.springboot;

import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DataBus;
import com.yomahub.liteflow.monitor.MonitorBus;
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
    public ComponentScaner componentScaner(){
        return new ComponentScaner();
    }

    @Bean
    public FlowExecutor flowExecutor(LiteflowProperty property){
        if(StringUtils.isNotBlank(property.getRuleSource())){
            List<String> ruleList = Lists.newArrayList(property.getRuleSource().split(","));
            FlowExecutor flowExecutor = new FlowExecutor();
            flowExecutor.setRulePath(ruleList);

            DataBus.setSlotSize(property.getSlotSize());

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
    public MonitorBus monitorBus(LiteflowMonitorProperty property){
        return new MonitorBus(property.isEnableLog(), property.getQueueLimit(), property.getDelay(), property.getPeriod());
    }
}
