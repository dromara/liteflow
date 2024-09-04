package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.exception.NoSwitchTargetNodeException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AppContext;
import org.noear.solon.test.SolonTest;

/**
 * 流程执行异常 单元测试
 *
 * @author zendwang
 */
@SolonTest
@Import(profiles="classpath:/exception/application.properties")
public class Exception2ELSpringBootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Inject
	private AppContext context;

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
