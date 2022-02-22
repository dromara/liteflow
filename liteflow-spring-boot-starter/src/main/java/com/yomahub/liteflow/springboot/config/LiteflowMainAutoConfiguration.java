package com.yomahub.liteflow.springboot.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.spi.spring.SpringAware;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.springboot.LiteflowExecutorInit;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * 主要的业务装配器
 * 在这个装配器里装配了执行器，执行器初始化类，监控器
 * 这个装配前置条件是需要LiteflowConfig，LiteflowPropertyAutoConfiguration以及SpringAware
 *
 * @author Bryan.Zhang
 */
@Configuration
@AutoConfigureAfter({LiteflowPropertyAutoConfiguration.class})
@ConditionalOnBean(LiteflowConfig.class)
@ConditionalOnProperty(prefix = "liteflow", name = "enable", havingValue = "true")
@Import(SpringAware.class)
public class LiteflowMainAutoConfiguration {

    @Bean
    public ComponentScanner componentScanner(LiteflowConfig liteflowConfig){
        return new ComponentScanner(liteflowConfig);
    }

    @Bean
    public FlowExecutor flowExecutor(LiteflowConfig liteflowConfig) {
        return new FlowExecutor(liteflowConfig);
    }

    @Bean
    @ConditionalOnProperty(prefix = "liteflow", name = "parse-on-start", havingValue = "true")
    public LiteflowExecutorInit liteflowExecutorInit(FlowExecutor flowExecutor) {
        return new LiteflowExecutorInit(flowExecutor);
    }

    @Bean
    @ConditionalOnProperty(prefix = "liteflow", name = "monitor.enable-log", havingValue = "true")
    public MonitorBus monitorBus(LiteflowConfig liteflowConfig) {
        return new MonitorBus(liteflowConfig);
    }
}
