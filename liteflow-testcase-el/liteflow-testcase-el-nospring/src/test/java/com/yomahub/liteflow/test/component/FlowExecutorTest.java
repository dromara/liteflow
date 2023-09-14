package com.yomahub.liteflow.test.component;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 组件功能点测试 单元测试
 *
 * @author donguo.tao
 */
public class FlowExecutorTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("component/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// isAccess方法的功能测试
	@Test
	public void testIsAccess() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", 101);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertNotNull(response.getSlot().getResponseData());
	}

	// 组件抛错的功能点测试
	@Test
	public void testComponentException() throws Throwable {
		Assertions.assertThrows(ArithmeticException.class, () -> {
			LiteflowResponse response = flowExecutor.execute2Resp("chain2", 0);
			Assertions.assertFalse(response.isSuccess());
			Assertions.assertEquals("/ by zero", response.getMessage());
			throw response.getCause();
		});
	}

	// isContinueOnError方法的功能点测试
	@Test
	public void testIsContinueOnError() throws Throwable {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", 0);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertNull(response.getCause());
	}

	// isEnd方法的功能点测试
	@Test
	public void testIsEnd() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", 10);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("d", response.getExecuteStepStr());
	}

	// setIsEnd方法的功能点测试
	@Test
	public void testSetIsEnd1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", 10);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("e", response.getExecuteStepStr());
	}

	// 条件组件的功能点测试
	@Test
	public void testNodeCondComponent() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", 0);
		Assertions.assertTrue(response.isSuccess());
	}

	// 测试setIsEnd如果为true，continueError也为true，那不应该continue了
	@Test
	public void testSetIsEnd2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", 10);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("g", response.getExecuteStepStr());
	}

	// 测试setIsEnd如果为true，continueError也为true，那不应该continue了,并且都在同一个 cmp 中
	@Test
	public void testSetIsEnd3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain8", 10);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("i", response.getExecuteStepStr());
	}

}
