package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.spring.ComponentScaner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiteflowComponentScanerAutoConfiguration {

    @Bean
    public ComponentScaner componentScaner(){
        return new ComponentScaner();
    }
}
