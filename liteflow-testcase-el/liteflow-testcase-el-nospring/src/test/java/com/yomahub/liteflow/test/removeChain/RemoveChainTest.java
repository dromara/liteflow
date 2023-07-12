package com.yomahub.liteflow.test.removeChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class RemoveChainTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("removeChain/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testRemoveChain() {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response1.isSuccess());
		FlowBus.removeChain("chain1");
		LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertFalse(response2.isSuccess());
	}

}
