package com.yomahub.liteflow.test.cmpStep;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/cmpStep/application.xml")
public class CmpStepSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testStep1(){
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(response.getExecuteSteps().get("a").isSuccess());
        Assert.assertTrue(response.getExecuteSteps().get("b").isSuccess());
        Assert.assertFalse(response.getExecuteSteps().get("c").isSuccess());
        Assert.assertFalse(response.getExecuteSteps().get("d").isSuccess());
        Assert.assertTrue(response.getExecuteSteps().get("c").getTimeSpent() >= 2000);
        Assert.assertEquals(RuntimeException.class, response.getExecuteSteps().get("c").getException().getClass());
        Assert.assertEquals(RuntimeException.class, response.getExecuteSteps().get("d").getException().getClass());
    }

    @Test
    public void testStep2() throws Exception{
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b", response.getExecuteStepStrWithoutTime());
    }
}
