package com.yomahub.liteflow.test.contextBean;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.contextBean.context.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

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

	// new一个上下文的情况
	@Test
	public void testContextBean2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg", new TestContext("J001", "test"));
		Assertions.assertTrue(response.isSuccess());
		TestContext context = response.getContextBean("skuContext");
		Assertions.assertEquals("J001", context.getSkuCode());
	}
}
