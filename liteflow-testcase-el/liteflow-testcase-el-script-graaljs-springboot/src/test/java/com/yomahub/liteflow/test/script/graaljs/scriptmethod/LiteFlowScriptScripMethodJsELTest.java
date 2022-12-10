package com.yomahub.liteflow.test.script.graaljs.scriptmethod;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/scriptmethod/application.properties")
@SpringBootTest(classes = LiteFlowScriptScripMethodJsELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.graaljs.scriptmethod.cmp", "com.yomahub.liteflow.test.script.graaljs.scriptmethod.bean"})
public class LiteFlowScriptScripMethodJsELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testScriptBean1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assert.assertEquals("hello", context.getData("demo"));
    }

    @Test
    public void testScriptBean2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assert.assertEquals("hello,kobekobe", context.getData("demo"));
    }

}
