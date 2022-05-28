package com.yomahub.liteflow.test.nodeExecutor;

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


/**
 * 测试spring下的组件重试
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/nodeExecutor/application.xml")
public class LiteflowNodeExecutorSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 默认执行器测试
    @Test
    public void testCustomerDefaultNodeExecutor() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(CustomerDefaultNodeExecutor.class, response.getContextBean().getData("customerDefaultNodeExecutor"));
        Assert.assertEquals("a", response.getExecuteStepStr());
    }

    //默认执行器测试+全局重试配置测试
    @Test
    public void testDefaultExecutorForRetry() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(CustomerDefaultNodeExecutor.class, response.getContextBean().getData("customerDefaultNodeExecutor"));
        Assert.assertEquals("b==>b==>b", response.getExecuteStepStr());
    }

    //自定义执行器测试
    @Test
    public void testCustomerExecutor() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("c", response.getExecuteStepStr());
    }

    //自定义执行器测试+全局重试配置测试
    @Test
    public void testCustomExecutorForRetry() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(CustomerNodeExecutorAndCustomRetry.class, response.getContextBean().getData("retryLogic"));
        Assert.assertEquals("d==>d==>d==>d==>d==>d", response.getExecuteStepStr());
    }
}
