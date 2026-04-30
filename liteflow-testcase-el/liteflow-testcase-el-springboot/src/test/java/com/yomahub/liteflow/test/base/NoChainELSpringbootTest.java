package com.yomahub.liteflow.test.base;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境下没有chain的启动测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/base/noChain/application.properties")
@SpringBootTest(classes = NoChainELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.base.cmp" })
public class NoChainELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testStartWithoutChain() {
		Assertions.assertNotNull(flowExecutor);
		Assertions.assertTrue(FlowBus.getChainMap().isEmpty());
	}

}
