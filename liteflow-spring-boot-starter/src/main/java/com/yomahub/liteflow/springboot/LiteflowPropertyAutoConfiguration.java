package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.property.LiteflowConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@EnableConfigurationProperties({LiteflowProperty.class,LiteflowMonitorProperty.class})
@ConditionalOnProperty(prefix = "liteflow", name = "rule-source")
@PropertySource(
        name = "Liteflow Default Properties",
        value = "classpath:/META-INF/liteflow-default.properties")
public class LiteflowPropertyAutoConfiguration {

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
}
