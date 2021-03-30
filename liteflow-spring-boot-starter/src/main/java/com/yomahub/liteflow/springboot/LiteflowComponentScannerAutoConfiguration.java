package com.yomahub.liteflow.springboot;

import com.yomahub.liteflow.spring.ComponentScaner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * 组件扫描器自动装配类
 * @author Bryan.Zhang
 */
@Configuration
@EnableAspectJAutoProxy(exposeProxy = true)
public class LiteflowComponentScannerAutoConfiguration {

    @Bean
    public ComponentScaner componentScaner(){
        return new ComponentScaner();
    }
}
