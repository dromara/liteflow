package com.yomahub.liteflow.test.retry;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;


@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/retry/application.properties")
@SpringBootTest(classes = RetryELDeclMultiSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.retry.cmp"})
public class RetryELDeclMultiSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// THEN测试
	@Test
	public void testThen() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>a==>b==>a==>b==>a==>b", response.getExecuteStepStr());
	}

	// WHEN测试
	@Test
	public void testWhen() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// node测试
	@Test
	public void testNode() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// FOR测试
	@Test
	public void testFor() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("c==>c==>c==>c==>a", response.getExecuteStepStr());
	}

	// SWITCH测试
	@Test
	public void testSwitch() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("d==>d==>d==>d==>a", response.getExecuteStepStr());
	}

	// IF测试
	@Test
	public void testIf() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("f==>f==>f==>f==>a", response.getExecuteStepStr());
	}

	// WHILE测试
	@Test
	public void testWhile() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("n==>n==>n==>n==>a==>n", response.getExecuteStepStr());
	}

	// ITERATOR测试
	@Test
	public void testIterator() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain8", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("i==>i==>i==>i==>a", response.getExecuteStepStr());
	}

	// 重试失败提示信息测试
	@Test
	public void testRetryFail() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain9", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("a==>b==>a==>b", response.getExecuteStepStr());
	}

	// FINALLY测试
	@Test
	public void testFinally() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain10", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("a==>b", response.getExecuteStepStr());
	}

	// 指定异常重试测试1
	@Test
	public void testException1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 指定异常重试测试2
	@Test
	public void testException2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain12", "arg");
		Assertions.assertFalse(response.isSuccess());
	}

}
