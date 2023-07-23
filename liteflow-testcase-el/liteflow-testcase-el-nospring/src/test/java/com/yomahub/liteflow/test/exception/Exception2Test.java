package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.exception.NoSwitchTargetNodeException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 流程执行异常 单元测试
 *
 * @author zendwang
 */
public class Exception2Test extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("exception/flow.el.xml");
		config.setWhenMaxWaitSeconds(1);
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testChainNotFoundException() throws Exception {
		Assertions.assertThrows(ChainNotFoundException.class, () -> {
			flowExecutor.execute("chain0", "it's a request");
		});
	}

	@Test
	public void testComponentCustomException() throws Exception {
		Assertions.assertThrows(RuntimeException.class, () -> {
			flowExecutor.execute("chain1", "exception");
		});
	}

	@Test
	public void testNoConditionInChainException() throws Throwable {
		Assertions.assertThrows(FlowSystemException.class, () -> {
			LiteFlowChainELBuilder.createChain().setChainId("chain2").build();
			LiteflowResponse response = flowExecutor.execute2Resp("chain2", "test");
			Assertions.assertFalse(response.isSuccess());
			Assertions.assertEquals("no conditionList in this chain[chain2]", response.getMessage());
			throw response.getCause();
		});
	}

	@Test
	public void testGetSlotFromResponseWhenException() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "test");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertNotNull(response.getCause());
		Assertions.assertNotNull(response.getSlot());
	}

	@Test
	public void testNoTargetFindException() throws Exception {
		Assertions.assertThrows(NoSwitchTargetNodeException.class, () -> {
			LiteflowResponse response = flowExecutor.execute2Resp("chain5", "test");
			Assertions.assertFalse(response.isSuccess());
			throw response.getCause();
		});
	}

	@Test
	public void testInvokeCustomStatefulException() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "custom-stateful-exception");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("300", response.getCode());
		Assertions.assertNotNull(response.getCause());
		Assertions.assertTrue(response.getCause() instanceof LiteFlowException);
		Assertions.assertNotNull(response.getSlot());
	}

	@Test
	public void testNotInvokeCustomStatefulException() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "test");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertNull(response.getCode());
	}

}
