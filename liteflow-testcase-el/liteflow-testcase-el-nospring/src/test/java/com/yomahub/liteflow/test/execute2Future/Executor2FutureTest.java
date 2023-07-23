package com.yomahub.liteflow.test.execute2Future;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.Future;

/**
 * nospring环境执行返回future的例子
 *
 * @author Bryan.Zhang
 * @since 2.6.13
 */
public class Executor2FutureTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("execute2Future/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testFuture() throws Exception {
		Future<LiteflowResponse> future = flowExecutor.execute2Future("chain1", "arg", DefaultContext.class);
		LiteflowResponse response = future.get();
		Assertions.assertTrue(response.isSuccess());
	}

}
