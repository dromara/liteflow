package com.yomahub.liteflow.springboot;

import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.spring.ComponentScaner;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
