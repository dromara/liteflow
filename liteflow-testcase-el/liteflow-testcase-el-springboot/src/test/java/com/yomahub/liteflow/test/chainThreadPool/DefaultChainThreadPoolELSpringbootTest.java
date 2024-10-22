package com.yomahub.liteflow.test.chainThreadPool;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境下chain线程池隔离测试
 */
@TestPropertySource(value = "classpath:/chainThreadPool/application.properties")
@SpringBootTest(classes = DefaultChainThreadPoolELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.chainThreadPool.cmp"})
public class DefaultChainThreadPoolELSpringbootTest extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	private FlowExecutor flowExecutor;

	/**
	 * 测试chain默认线程池隔离
	 */
	@Test
	public void testDefaultChainThreadPool() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertTrue(context.getData("threadNameFor").toString().startsWith("chain-thread-"));
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("chain-thread-"));

	}

}
