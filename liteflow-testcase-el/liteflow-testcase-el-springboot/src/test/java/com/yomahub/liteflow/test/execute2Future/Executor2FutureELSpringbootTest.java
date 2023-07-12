package com.yomahub.liteflow.test.execute2Future;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.concurrent.Future;

/**
 * springboot环境执行返回future的例子
 *
 * @author Bryan.Zhang
 * @since 2.6.13
 */
@TestPropertySource(value = "classpath:/execute2Future/application.properties")
@SpringBootTest(classes = Executor2FutureELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.execute2Future.cmp" })
public class Executor2FutureELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testFuture() throws Exception {
		Future<LiteflowResponse> future = flowExecutor.execute2Future("chain1", "arg", DefaultContext.class);
		LiteflowResponse response = future.get();
		Assertions.assertTrue(response.isSuccess());
	}

}
