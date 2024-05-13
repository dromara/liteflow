package com.yomahub.liteflow.test.script.kotlin.scriptbean;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ScriptBeanMethodInvokeException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.script.ScriptBeanManager;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.script.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/scriptbean/application.properties")
@SpringBootTest(classes = LiteFlowScriptScriptbeanKotlinELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.kotlin.scriptbean.cmp",
		"com.yomahub.liteflow.test.script.kotlin.scriptbean.bean" })
public class LiteFlowScriptScriptbeanKotlinELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testScriptBean1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello", context.getData("demo"));
	}

	@Test
	public void testScriptBean2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello,kobe", context.getData("demo"));
	}

	// 测试scriptBean includeMethodName配置包含情况下
	@Test
	public void testScriptBean3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello,kobe", context.getData("demo"));
	}

	// 测试scriptBean includeMethodName配置不包含情况下
	@Test
	public void testScriptBean4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals(ScriptBeanMethodInvokeException.class, response.getCause().getClass());
	}

	// 测试scriptBean excludeMethodName配置不包含情况下
	@Test
	public void testScriptBean5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello,kobe", context.getData("demo"));
	}

	// 测试scriptBean excludeMethodName配置包含情况下
	@Test
	public void testScriptBean6() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals(ScriptBeanMethodInvokeException.class, response.getCause().getClass());
	}

	// 测试在ScriptBeanManager里放入上下文，实现自定义脚本引用名称
	@Test
	public void testScriptBean7() throws Exception {
		Map<String, String> map = new HashMap<>();
		ScriptBeanManager.addScriptBean("abcCx", map);
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg", map);
		Assertions.assertTrue(response.isSuccess());
		Map<String, String> context = response.getFirstContextBean();
		Assertions.assertEquals("hello", context.get("demo"));
	}

	//测试用构造方法的方式注入bean的场景
	@Test
	public void testScriptBean8() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain8", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello,jordan", context.getData("demo"));
	}

}
