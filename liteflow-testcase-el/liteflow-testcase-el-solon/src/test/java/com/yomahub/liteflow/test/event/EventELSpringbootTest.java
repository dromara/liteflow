package com.yomahub.liteflow.test.event;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * solon环境事件回调测试
 *
 * @author Bryan.Zhang
 * @since 2.7.1
 */
@SolonTest
@Import(profiles="classpath:/event/application.properties")
public class EventELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

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
