package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;


/**
 * 测试springboot下的组件重试
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/nodeExecutor/application.properties")
public class LiteflowNodeExecutorELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    // 默认执行器测试
    @Test
    public void testCustomerDefaultNodeExecutor() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(CustomerDefaultNodeExecutor.class, context.getData("customerDefaultNodeExecutor"));
        Assert.assertEquals("a", response.getExecuteStepStr());
    }

    //默认执行器测试+全局重试配置测试
    @Test
    public void testDefaultExecutorForRetry() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(CustomerDefaultNodeExecutor.class, context.getData("customerDefaultNodeExecutor"));
        Assert.assertEquals("b==>b==>b", response.getExecuteStepStr());
    }

    //自定义执行器测试
    @Test
    public void testCustomerExecutor() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("c", response.getExecuteStepStr());
    }

    //自定义执行器测试+全局重试配置测试
    @Test
    public void testCustomExecutorForRetry() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(CustomerNodeExecutorAndCustomRetry.class, context.getData("retryLogic"));
        Assert.assertEquals("d==>d==>d==>d==>d==>d", response.getExecuteStepStr());
    }
}
