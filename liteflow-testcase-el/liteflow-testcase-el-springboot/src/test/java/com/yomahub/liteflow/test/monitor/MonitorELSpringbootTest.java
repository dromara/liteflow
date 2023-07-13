package com.yomahub.liteflow.test.monitor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
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
@TestPropertySource(value = "classpath:/monitor/application.properties")
@SpringBootTest(classes = MonitorELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.monitor.cmp" })
public class MonitorELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testMonitor() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());

		Thread.sleep(10000);
	}

	@AfterAll
	public static void clean() {
		MonitorBus monitorBus = ContextAwareHolder.loadContextAware().getBean(MonitorBus.class);
		monitorBus.closeScheduler();
	}

}
