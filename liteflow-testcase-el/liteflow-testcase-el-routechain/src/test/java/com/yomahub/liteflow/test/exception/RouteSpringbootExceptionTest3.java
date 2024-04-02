package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.exception.RouteChainNotFoundException;
import com.yomahub.liteflow.exception.RouteELInvalidException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
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
@SpringBootTest(classes = RouteSpringbootExceptionTest3.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.exception.cmp" })
public class RouteSpringbootExceptionTest3 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 没有定义route
	@Test
	public void testExceptionRoute() throws Exception {
		Assertions.assertThrows(RouteELInvalidException.class,() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("exception/flow3.xml");
			flowExecutor.reloadRule();
		});
	}
}
