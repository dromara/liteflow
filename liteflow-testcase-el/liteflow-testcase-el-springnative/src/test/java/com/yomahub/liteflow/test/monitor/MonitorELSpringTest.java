package com.yomahub.liteflow.test.monitor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.monitor.MonitorBus;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * spring环境下监控的测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/monitor/application.xml")
public class MonitorELSpringTest extends BaseTest {

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
