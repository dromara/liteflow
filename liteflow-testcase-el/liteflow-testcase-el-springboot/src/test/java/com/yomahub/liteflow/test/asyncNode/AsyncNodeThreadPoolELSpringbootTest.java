package com.yomahub.liteflow.test.asyncNode;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@TestPropertySource(value = "classpath:/asyncNode/application2.properties")
@SpringBootTest(classes = AsyncNodeThreadPoolELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.asyncNode.cmp" })
public class AsyncNodeThreadPoolELSpringbootTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testAsyncFlow1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a base request");
        Assertions.assertTrue(response.isSuccess());
    }
}
