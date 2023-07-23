package com.yomahub.liteflow.test.multiContext;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoSuchContextBeanException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境最普通的例子测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@TestPropertySource(value = "classpath:/multiContext/application.properties")
@SpringBootTest(classes = MultiContextELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.multiContext.cmp" })
public class MultiContextELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testMultiContext1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class);
		OrderContext orderContext = response.getContextBean(OrderContext.class);
		CheckContext checkContext = response.getContextBean(CheckContext.class);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("987XYZ", checkContext.getSign());
		Assertions.assertEquals(95, checkContext.getRandomId());
		Assertions.assertEquals("SO12345", orderContext.getOrderNo());
		Assertions.assertEquals(2, orderContext.getOrderType());
		Assertions.assertEquals(DateUtil.parseDate("2022-06-15"), orderContext.getCreateTime());
	}

	@Test
	public void testMultiContext2() throws Exception {
		Assertions.assertThrows(NoSuchContextBeanException.class, () -> {
			LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class);
			DefaultContext context = response.getContextBean(DefaultContext.class);
		});
	}

	@Test
	public void testPassInitializedContextBean() throws Exception {
		OrderContext orderContext = new OrderContext();
		orderContext.setOrderNo("SO11223344");
		CheckContext checkContext = new CheckContext();
		checkContext.setSign("987654321d");
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", null, orderContext, checkContext);
		Assertions.assertTrue(response.isSuccess());
		OrderContext context1 = response.getContextBean(OrderContext.class);
		CheckContext context2 = response.getContextBean(CheckContext.class);
		Assertions.assertEquals("SO11223344", context1.getOrderNo());
		Assertions.assertEquals("987654321d", context2.getSign());
	}

}
