package com.yomahub.liteflow.test.aop;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TestRunner {
    public static void main(String[] args) {
        try{
            SpringApplication.run(TestRunner.class, args);
            System.exit(0);
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
