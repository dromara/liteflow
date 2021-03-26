package com.yomahub.flowtest.concurrent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

/**
 * 启动类
 * @author justin.xu
 */
@SpringBootApplication(exclude={DataSourceAutoConfiguration.class})
public class SpringBootApp {
    /**
     * @param args
     */
    public static void main(String[] args) {
        SpringApplication.run(SpringBootApp.class, args);
    }

}
