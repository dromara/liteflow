package com.yomahub.liteflow.test.event;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境最普通的例子测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/event/application.properties")
@SpringBootTest(classes = EventELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.event.cmp" })
public class EventELDeclSpringbootTest extends BaseTest {

	@Resource
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
