package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoMatchedRouteChainException;
import com.yomahub.liteflow.exception.RouteChainNotFoundException;
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
@TestPropertySource(value = "classpath:/exception/application2.properties")
@SpringBootTest(classes = RouteSpringbootExceptionTest2.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.exception.cmp" })
public class RouteSpringbootExceptionTest2 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 没有定义route
	@Test
	public void testExceptionRoute() throws Exception {
		Assertions.assertThrows(RouteChainNotFoundException.class,
				() -> flowExecutor.executeRouteChain(1, DefaultContext.class)
		);
	}
}
