package com.yomahub.liteflow.test.script.python.common.domain;

import com.yomahub.liteflow.script.annotation.ScriptBean;
import org.springframework.stereotype.Component;

@Component
@ScriptBean("td")
public class TestDomain {

    public String sayHi(String name){
        return "hi," + name;
    }
}
