package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * spring环境下参数单元测试
 *
 * @author zendwang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
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
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("config/flow.el.json", config.getRuleSource());
		Assertions.assertEquals(15000, config.getWhenMaxWaitTime().intValue());
		Assertions.assertEquals(TimeUnit.MILLISECONDS, config.getWhenMaxWaitTimeUnit());
		Assertions.assertEquals(200, config.getQueueLimit().intValue());
		Assertions.assertEquals(300000L, config.getDelay().longValue());
		Assertions.assertEquals(300000L, config.getPeriod().longValue());
		Assertions.assertFalse(config.getEnableLog());
		// Assertions.assertEquals(Runtime.getRuntime().availableProcessors() * 2,
		// config.getWhenMaxWorkers().longValue());
        Assertions.assertEquals(512, config.getGlobalThreadPoolQueueSize().longValue());
	}

}
