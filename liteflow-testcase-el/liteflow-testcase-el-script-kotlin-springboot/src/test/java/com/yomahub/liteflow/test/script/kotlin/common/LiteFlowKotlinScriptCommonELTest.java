package com.yomahub.liteflow.test.script.kotlin.common;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.script.BaseTest;
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
@SpringBootTest(classes = LiteFlowKotlinScriptCommonELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.kotlin.common.cmp"})
public class LiteFlowKotlinScriptCommonELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testCommon1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertEquals(Integer.valueOf(5), context.getData("s1"));
    }

    @Test
    public void testFor1() {
        DefaultContext context = new DefaultContext();
        context.setData("k1", 1);
        context.setData("k2", 2);
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg", context);
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("s2==>a==>a==>a",response.getExecuteStepStr());
    }

    @Test
    public void testIf1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("s3==>b",response.getExecuteStepStr());
    }

    @Test
    public void testIf2() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("s4",response.getExecuteStepStr());
    }

    @Test
    public void testSwitch1() {
        DefaultContext context = new DefaultContext();
        context.setData("id", "c");
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg", context);
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("s5==>c",response.getExecuteStepStr());
    }
}
