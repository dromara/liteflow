package com.yomahub.liteflow.test.cmpStep;

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

/**
 * springboot环境最普通的例子测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/cmpStep/application.properties")
@SpringBootTest(classes = CmpStepSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.cmpStep.cmp"})
public class CmpStepSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testStep() throws Exception{
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

}
