package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@TestPropertySource(value = "classpath:/subflow/nestedImplicitSubFlow.properties")
@SpringBootTest(classes = SubflowXMLELSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.subflow.cmp1","com.yomahub.liteflow.test.subflow.cmp3" })
public class NestedImplicitSubFlowTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testNested(){
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
        Assertions.assertTrue(response.isSuccess());
    }

}
