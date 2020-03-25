package com.yomahub.flowtest;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.Slot;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TestFlow implements CommandLineRunner {

    @Resource
    private FlowExecutor flowExecutor;

    @Override
    public void run(String... args) throws Exception {
        Slot slot = flowExecutor.execute("chain3", "it's a request");
        System.out.println(slot);
    }
}
