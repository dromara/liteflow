package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.exception.NoSwitchTargetNodeException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import javax.annotation.Resource;

/**
 * 流程执行异常 单元测试
 *
 * @author zendwang
 */
@TestPropertySource(value = "classpath:/exception/application.properties")
@SpringBootTest(classes = Exception2ELSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.exception.cmp" })
public class Exception2ELSpringBootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testChainNotFoundException() throws Exception {
		Assertions.assertThrows(ChainNotFoundException.class, () -> flowExecutor.execute("chain0", "it's a request"));
	}

	@Test
	public void testComponentCustomException() throws Exception {
		Assertions.assertThrows(RuntimeException.class, new Executable() {
			@Override
			public void execute() throws Throwable {
				flowExecutor.execute("chain1", "exception");
			}
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
