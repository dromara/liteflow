package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * 非spring环境下参数单元测试
 *
 * @author zendwang
 * @since 2.5.0
 */
public class LiteflowConfigTest1 extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeClass
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("config/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testConfig() {
		LiteflowConfig config = LiteflowConfigGetter.get();
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("config/flow.el.xml", config.getRuleSource());
		Assert.assertEquals(15000, config.getWhenMaxWaitTime().intValue());
		Assert.assertEquals(TimeUnit.MILLISECONDS, config.getWhenMaxWaitTimeUnit());
		Assert.assertEquals(200, config.getQueueLimit().intValue());
		Assert.assertEquals(300000L, config.getDelay().longValue());
		Assert.assertEquals(300000L, config.getPeriod().longValue());
		Assert.assertFalse(config.getEnableLog());
		Assert.assertEquals(16, config.getWhenMaxWorkers().longValue());
		Assert.assertEquals(512, config.getWhenQueueLimit().longValue());
		Assert.assertEquals(true, config.isParseOnStart());
	}

}
