package com.yomahub.liteflow.test.multiContext;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoSuchContextBeanException;
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
 * springboot环境最普通的例子测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/multiContext/application.properties")
public class MultiContextELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testMultiContext1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class);
        OrderContext orderContext = response.getContextBean(OrderContext.class);
        CheckContext checkContext = response.getContextBean(CheckContext.class);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("987XYZ", checkContext.getSign());
        Assert.assertEquals(95, checkContext.getRandomId());
        Assert.assertEquals("SO12345", orderContext.getOrderNo());
        Assert.assertEquals(2, orderContext.getOrderType());
        Assert.assertEquals(DateUtil.parseDate("2022-06-15"), orderContext.getCreateTime());
    }

    @Test(expected = NoSuchContextBeanException.class)
    public void testMultiContext2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class);
        DefaultContext context = response.getContextBean(DefaultContext.class);
    }

    @Test
    public void testPassInitializedContextBean() throws Exception{
        OrderContext orderContext = new OrderContext();
        orderContext.setOrderNo("SO11223344");
        CheckContext checkContext = new CheckContext();
        checkContext.setSign("987654321d");
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", null, orderContext, checkContext);
        Assert.assertTrue(response.isSuccess());
        OrderContext context1 = response.getContextBean(OrderContext.class);
        CheckContext context2 = response.getContextBean(CheckContext.class);
        Assert.assertEquals("SO11223344", context1.getOrderNo());
        Assert.assertEquals("987654321d", context2.getSign());
    }

}
