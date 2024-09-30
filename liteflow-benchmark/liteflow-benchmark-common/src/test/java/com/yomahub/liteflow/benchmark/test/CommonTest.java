package com.yomahub.liteflow.benchmark.test;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.properties")
@SpringBootTest(classes = CommonTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.benchmark.cmp" })
public class CommonTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void test1() {
        flowExecutor.execute2Resp("chain1");
    }
}
