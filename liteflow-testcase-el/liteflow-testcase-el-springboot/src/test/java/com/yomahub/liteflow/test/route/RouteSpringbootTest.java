package com.yomahub.liteflow.test.route;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/route/application.properties")
@SpringBootTest(classes = RouteSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.route.cmp" })
public class RouteSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testRoute1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("r_chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	@Test
	public void testRoute2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("r_chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
