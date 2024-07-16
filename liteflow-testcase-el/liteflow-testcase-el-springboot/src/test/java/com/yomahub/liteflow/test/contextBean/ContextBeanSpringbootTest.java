package com.yomahub.liteflow.test.contextBean;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.contextBean.context.TestContext;
import com.yomahub.liteflow.test.contextBean.context.TestSubContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * ContextBean测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/contextBean/application.properties")
@SpringBootTest(classes = ContextBeanSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.contextBean.cmp" })
public class ContextBeanSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testContextBean1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", TestContext.class);
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getContextBean("skuContext");
		Assertions.assertEquals("J001", context.getSkuCode());
	}

	// 利用别名取上下文
	@Test
	public void testContextBean2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", new TestContext("J001", "test"));
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getContextBean("skuContext");
		Assertions.assertEquals("J001", context.getSkuCode());
	}

	// 利用超类取上下文
	@Test
	public void testContextBean3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg", new TestSubContext("J001", "test", new BigDecimal("10.5")));
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getContextBean(TestContext.class);
		Assertions.assertEquals("J001", context.getSkuCode());
	}
}
