package com.yomahub.liteflow.test.customWhenThreadPool;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * springboot环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/customWhenThreadPool/application.properties")
@SpringBootTest(classes = CustomWhenThreadPoolELDeclMultiSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.customWhenThreadPool.cmp" })
public class CustomWhenThreadPoolELDeclMultiSpringbootTest extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	private FlowExecutor flowExecutor;

	/**
	 * 测试全局线程池配置
	 */
	@Test
	public void testGlobalThreadPool() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("global-thread-1"));
	}

	/**
	 * 测试全局和when上自定义线程池-优先以when上为准
	 */
	@Test
	public void testGlobalAndCustomWhenThreadPool() {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response1.getFirstContextBean();
		Assertions.assertTrue(response1.isSuccess());
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-when-1-thead"));
	}

	/**
	 * when配置的线程池可以共用
	 */
	@Test
	public void testCustomWhenThreadPool() {
		// 使用when - thread1
		testGlobalAndCustomWhenThreadPool();
		// chain配置同一个thead1
		LiteflowResponse response2 = flowExecutor.execute2Resp("chain2", "arg");
		DefaultContext context = response2.getFirstContextBean();
		Assertions.assertTrue(response2.isSuccess());
		Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-when-1-thead"));

	}

}
