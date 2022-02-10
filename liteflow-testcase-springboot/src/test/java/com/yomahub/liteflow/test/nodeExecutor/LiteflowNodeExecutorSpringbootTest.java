package com.yomahub.liteflow.test.nodeExecutor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.executor.DefaultNodeExecutor;
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
 * 测试springboot下的组件重试
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/nodeExecutor/application.properties")
@SpringBootTest(classes = LiteflowNodeExecutorSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.nodeExecutor.cmp"})
public class LiteflowNodeExecutorSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 默认执行器测试
    @Test
    public void testCustomerDefaultNodeExecutor() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(CustomerDefaultNodeExecutor.class, response.getSlot().getData("customerDefaultNodeExecutor"));
        Assert.assertEquals("a", response.getSlot().getExecuteStepStr());
    }

    //默认执行器测试+全局重试配置测试
    @Test
    public void testDefaultExecutorForRetry() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(CustomerDefaultNodeExecutor.class, response.getSlot().getData("customerDefaultNodeExecutor"));
        Assert.assertEquals("b==>b==>b", response.getSlot().getExecuteStepStr());
    }

    //自定义执行器测试
    @Test
    public void testCustomerExecutor() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("c", response.getSlot().getExecuteStepStr());
    }

    //自定义执行器测试+全局重试配置测试
    @Test
    public void testCustomExecutorForRetry() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(CustomerNodeExecutorAndCustomRetry.class, response.getSlot().getData("retryLogic"));
        Assert.assertEquals("d==>d==>d==>d==>d==>d", response.getSlot().getExecuteStepStr());
    }
}
