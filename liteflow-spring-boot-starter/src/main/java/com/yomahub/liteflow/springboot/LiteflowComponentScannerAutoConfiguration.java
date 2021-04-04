package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.spring.ComponentScanner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 组件扫描器自动装配类
 * @author Bryan.Zhang
 */
@Configuration
public class LiteflowComponentScannerAutoConfiguration {

    @Bean
    public ComponentScanner componentScaner(){
        return new ComponentScanner();
    }
}
