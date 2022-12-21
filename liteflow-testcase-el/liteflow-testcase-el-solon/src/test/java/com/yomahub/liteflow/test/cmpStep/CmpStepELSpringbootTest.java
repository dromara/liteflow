package com.yomahub.liteflow.test.cmpStep;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.entity.CmpStep;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

import java.util.HashSet;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * springboot环境step的测试例子
 * @author Bryan.Zhang
 * @since 2.7.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/cmpStep/application.properties")
public class CmpStepELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //ab串行
    //cd并行，都抛错,其中c耗时2秒
    @Test
    public void testStep1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
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
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testStep3() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Map<String, CmpStep> stepMap = response.getExecuteSteps();
        Assert.assertEquals(2, stepMap.size());
        Queue<CmpStep> queue = response.getExecuteStepQueue();
        Assert.assertEquals(5, queue.size());

        Set<String> tagSet = new HashSet<>();
        response.getExecuteStepQueue().stream().filter(
                cmpStep -> cmpStep.getNodeId().equals("a")
        ).forEach(cmpStep -> tagSet.add(cmpStep.getTag()));

        Assert.assertEquals(3, tagSet.size());

    }

}
