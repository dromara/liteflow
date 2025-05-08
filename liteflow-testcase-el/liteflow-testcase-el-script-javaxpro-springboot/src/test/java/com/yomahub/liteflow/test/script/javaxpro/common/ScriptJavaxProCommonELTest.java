package com.yomahub.liteflow.test.script.javaxpro.common;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/common/application.properties")
@SpringBootTest(classes = ScriptJavaxProCommonELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javaxpro.common.cmp" })
public class ScriptJavaxProCommonELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testCommon1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(6, (int)context.getData("s1"));
        Assertions.assertEquals("hello,jack", context.getData("hi"));
        Assertions.assertEquals(47100, (Integer) context.getData("salary"));
    }

    @Test
    public void testCommon2() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
    }
}