package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * 测试非spring环境下的自定义组件执行器
 * @author Bryan.Zhang
 * @since 2.5.10
 */
public class LiteflowNodeExecutorTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("nodeExecutor/flow.el.xml");
        config.setRetryCount(3);
        config.setSlotSize(512);
        config.setNodeExecutorClass("com.yomahub.liteflow.test.nodeExecutor.CustomerDefaultNodeExecutor");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

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
