package com.yomahub.liteflow.test.switchcase;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境EL常规的例子测试
 * @author Bryan.Zhang
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/switchcase/application.properties")
public class SwitchELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //2022-07-12 switch 异常错误.c.y.l.builder.el.operator.ToOperator     : parameter error
    //run QlExpress Exception at line 1 :
    // switch().to(): 只有一个node时出错
    @Test
    public void testSwitch1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>e==>d==>b", response.getExecuteStepStr());
    }

    @Test
    public void testSwitch2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>e==>d",response.getExecuteStepStr());
    }

    @Test
    public void testSwitch3() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>f==>b",response.getExecuteStepStr());
    }

    //根据tag来跳转，指定哪个组件的tag
    @Test
    public void testSwitch4() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>g==>d",response.getExecuteStepStr());
    }

    //tag的跳转
    @Test
    public void testSwitch5() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>h==>b",response.getExecuteStepStr());
    }

    //相同组件的tag的跳转
    @Test
    public void testSwitch6() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>h==>b",response.getExecuteStepStr());
    }

    //switch增加default选项
    @Test
    public void testSwitch7() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>i==>d",response.getExecuteStepStr());
    }

}
