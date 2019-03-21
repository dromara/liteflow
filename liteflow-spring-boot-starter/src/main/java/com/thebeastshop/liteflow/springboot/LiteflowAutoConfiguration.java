package com.thebeastshop.liteflow.springboot;

import com.google.common.collect.Lists;
import com.thebeastshop.liteflow.core.FlowExecutor;
import com.thebeastshop.liteflow.spring.ComponentScaner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "liteflow.ruleSource")
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
            return flowExecutor;
        }else{
            return null;
        }
    }
}
