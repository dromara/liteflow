package com.yomahub.flowtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Runner {

    public static void main(String[] args) {
        try{
            SpringApplication.run(Runner.class, args);

            System.exit(0);
        }catch (Throwable t){
            t.printStackTrace();
        }
    }
}
