package com.yomahub.liteflow.test.event;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class EventTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("event/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 测试组件成功事件
	@Test
	public void testEvent1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("abc", context.getData("test"));
	}

	// 测试组件失败事件
	@Test
	public void testEvent2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals(NullPointerException.class, response.getCause().getClass());
		Assertions.assertEquals("ab", context.getData("test"));
		Assertions.assertEquals("error:d", context.getData("error"));
	}

	// 测试组件失败事件本身抛出异常
	@Test
	public void testEvent3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals(NullPointerException.class, response.getCause().getClass());
		Assertions.assertEquals("a", context.getData("test"));
		Assertions.assertEquals("error:e", context.getData("error"));
	}

}
