package com.yomahub.liteflow.test.script.kotlin.contextbean;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.script.BaseTest;
import com.yomahub.liteflow.test.script.kotlin.contextbean.bean.CheckContext;
import com.yomahub.liteflow.test.script.kotlin.contextbean.bean.Order2Context;
import com.yomahub.liteflow.test.script.kotlin.contextbean.bean.OrderContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/contextbean/application.properties")
@SpringBootTest(classes = LiteFlowScriptContextbeanKotlinELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.kotlin.contextbean.cmp",
		"com.yomahub.liteflow.test.script.kotlin.contextbean.bean" })
public class LiteFlowScriptContextbeanKotlinELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testContextBean1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", OrderContext.class, CheckContext.class,
				Order2Context.class);
		Assertions.assertTrue(response.isSuccess());
		OrderContext orderContext = response.getContextBean(OrderContext.class);
		CheckContext checkContext = response.getContextBean(CheckContext.class);
		Order2Context order2Context = response.getContextBean(Order2Context.class);
		Assertions.assertEquals("order1", orderContext.getOrderNo());
		Assertions.assertEquals("sign1", checkContext.getSign());
		Assertions.assertEquals("order2", order2Context.getOrderNo());
	}

	@Test
	public void testContextBean2() throws Exception {
		OrderContext orderContext = new OrderContext();
		orderContext.setOrderNo("order1");
		CheckContext checkContext = new CheckContext();
		checkContext.setSign("sign1");
		Order2Context orderContext2 = new Order2Context();
		orderContext2.setOrderNo("order2");
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", null, orderContext, checkContext,
				orderContext2);
		Assertions.assertTrue(response.isSuccess());
	}

}
