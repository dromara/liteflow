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
@TestPropertySource(value = "classpath:/multiContext/application.properties")
@SpringBootTest(classes = MultiContextELDeclMultiSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.multiContext.cmp"})
public class MultiContextELDeclMultiSpringbootTest extends BaseTest {

    @Resource
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

}
