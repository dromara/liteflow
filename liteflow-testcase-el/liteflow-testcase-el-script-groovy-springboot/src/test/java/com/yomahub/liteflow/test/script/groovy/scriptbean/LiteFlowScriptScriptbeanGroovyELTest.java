package com.yomahub.liteflow.test.script.groovy.scriptbean;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ScriptBeanMethodInvokeException;
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
@TestPropertySource(value = "classpath:/scriptbean/application.properties")
@SpringBootTest(classes = LiteFlowScriptScriptbeanGroovyELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.groovy.scriptbean.cmp","com.yomahub.liteflow.test.script.groovy.scriptbean.bean"})
public class LiteFlowScriptScriptbeanGroovyELTest extends BaseTest {

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
        Assert.assertEquals("hello,kobe", context.getData("demo"));
    }

    //测试scriptBean includeMethodName配置包含情况下
    @Test
    public void testScriptBean3() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assert.assertEquals("hello,kobe", context.getData("demo"));
    }

    //测试scriptBean includeMethodName配置不包含情况下
    @Test
    public void testScriptBean4() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(ScriptBeanMethodInvokeException.class, response.getCause().getClass());
    }

    //测试scriptBean excludeMethodName配置不包含情况下
    @Test
    public void testScriptBean5() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assert.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        Assert.assertEquals("hello,kobe", context.getData("demo"));
    }

    //测试scriptBean excludeMethodName配置包含情况下
    @Test
    public void testScriptBean6() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(ScriptBeanMethodInvokeException.class, response.getCause().getClass());
    }
}
