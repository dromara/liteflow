package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoMatchedRouteChainException;
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
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/exception/application1.properties")
@SpringBootTest(classes = RouteSpringbootExceptionTest1.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.exception.cmp" })
public class RouteSpringbootExceptionTest1 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 定义了但是没有路由满足
	@Test
	public void testExceptionRoute() throws Exception {
		Assertions.assertThrows(NoMatchedRouteChainException.class,
				() -> flowExecutor.executeRouteChain(1, DefaultContext.class)
		);
	}
}
