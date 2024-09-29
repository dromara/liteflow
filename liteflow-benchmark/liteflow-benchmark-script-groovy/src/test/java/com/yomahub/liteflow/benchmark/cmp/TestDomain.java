package com.yomahub.liteflow.benchmark.cmp;

import org.springframework.stereotype.Component;


@Component
public class TestDomain {

    public String sayHello(String name){
        return "hello," + name;
    }

}
