package com.yomahub.flowtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class FlowtestApplication {

    public static void main(String[] args) {
        try{
            SpringApplication.run(FlowtestApplication.class, args);

            System.exit(0);
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
