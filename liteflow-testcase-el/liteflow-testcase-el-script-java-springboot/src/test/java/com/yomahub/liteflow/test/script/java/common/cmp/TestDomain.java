package com.yomahub.liteflow.test.script.java.common.cmp;

import org.springframework.stereotype.Component;

@Component
public class TestDomain {

    public String sayHello(String name){
        return "hello," + name;
    }
}
