package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.ParseModeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

/**
 * 非spring环境下参数单元测试
 *
 * @author zendwang
 * @since 2.5.0
 */
public class LiteflowConfigTest1 extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("config/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testConfig() {
		LiteflowConfig config = LiteflowConfigGetter.get();
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("config/flow.el.xml", config.getRuleSource());
		Assertions.assertEquals(15000, config.getWhenMaxWaitTime().intValue());
		Assertions.assertEquals(TimeUnit.MILLISECONDS, config.getWhenMaxWaitTimeUnit());
		Assertions.assertEquals(200, config.getQueueLimit().intValue());
		Assertions.assertEquals(300000L, config.getDelay().longValue());
		Assertions.assertEquals(300000L, config.getPeriod().longValue());
		Assertions.assertFalse(config.getEnableLog());
        Assertions.assertEquals(64, config.getGlobalThreadPoolSize().longValue());
        Assertions.assertEquals(512, config.getGlobalThreadPoolQueueSize().longValue());
		Assertions.assertEquals(ParseModeEnum.PARSE_ALL_ON_START, config.getParseMode());
	}

}
