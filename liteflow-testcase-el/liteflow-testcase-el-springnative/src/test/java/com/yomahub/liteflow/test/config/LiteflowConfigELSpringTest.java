package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * spring环境下参数单元测试
 *
 * @author zendwang
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/config/application-local.xml")
public class LiteflowConfigELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testConfig() throws Exception {
		LiteflowConfig config = context.getBean(LiteflowConfig.class);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("config/flow.el.json", config.getRuleSource());
		Assert.assertEquals(15000, config.getWhenMaxWaitTime().intValue());
		Assert.assertEquals(TimeUnit.MILLISECONDS, config.getWhenMaxWaitTimeUnit());
		Assert.assertEquals(200, config.getQueueLimit().intValue());
		Assert.assertEquals(300000L, config.getDelay().longValue());
		Assert.assertEquals(300000L, config.getPeriod().longValue());
		Assert.assertFalse(config.getEnableLog());
		// Assert.assertEquals(Runtime.getRuntime().availableProcessors() * 2,
		// config.getWhenMaxWorkers().longValue());
		Assert.assertEquals(512, config.getWhenQueueLimit().longValue());
	}

}
