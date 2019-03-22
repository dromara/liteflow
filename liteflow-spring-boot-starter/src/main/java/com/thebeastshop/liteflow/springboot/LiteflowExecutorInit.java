package com.thebeastshop.liteflow.springboot;

import com.thebeastshop.liteflow.core.FlowExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiteflowExecutorInit {

    @Bean
    public String initExecutor(FlowExecutor flowExecutor){
        flowExecutor.init();
        return "init";
    }
}
