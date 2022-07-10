package com.yomahub.liteflow.test.aop;

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
 * 切面场景单元测试
 * @author zendwang
 * @since 2.8.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/aop/application-custom.xml")
public class CustomAOPELSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试自定义AOP，串行场景
    @Test
    public void testCustomAopS() {
        LiteflowResponse response= flowExecutor.execute2Resp("chain1", "it's a request");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", context.getData("a"));
        Assert.assertEquals("before_after", context.getData("b"));
        Assert.assertEquals("before_after", context.getData("c"));
    }

    //测试自定义AOP，并行场景
    @Test
    public void testCustomAopP() {
        LiteflowResponse response= flowExecutor.execute2Resp("chain2", "it's a request");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", context.getData("a"));
        Assert.assertEquals("before_after", context.getData("b"));
        Assert.assertEquals("before_after", context.getData("c"));
    }
}
