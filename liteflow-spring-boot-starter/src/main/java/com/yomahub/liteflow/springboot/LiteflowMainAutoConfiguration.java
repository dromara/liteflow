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
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.PropertySource;

import javax.swing.*;
import java.util.List;

@Configuration
@ConditionalOnBean(LiteflowConfig.class)
@AutoConfigureAfter(LiteflowPropertyAutoConfiguration.class)
@Import(SpringAware.class)
@PropertySource(
        name = "Liteflow Default Properties",
        value = "classpath:/META-INF/liteflow-default.properties")
public class LiteflowMainAutoConfiguration {

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
    public LiteflowExecutorInit liteflowExecutorInit(FlowExecutor flowExecutor){
        return new LiteflowExecutorInit(flowExecutor);
    }

    @Bean
    public MonitorBus monitorBus(LiteflowConfig liteflowConfig){
        return new MonitorBus(liteflowConfig);
    }
}
