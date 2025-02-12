package com.yomahub.liteflow.test.booleanOpt;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/booleanOpt/application.properties")
@SpringBootTest(classes = BooleanOptELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.booleanOpt.cmp" })
public class BooleanOptELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// IF情况下AND
	@Test
	public void testBooleanOpt1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x1==>x2==>x3==>b", response.getExecuteStepStr());
	}

	// IF情况下OR
	@Test
	public void testBooleanOpt2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x1==>a", response.getExecuteStepStr());
	}

	// IF情况下AND+NOT
	@Test
	public void testBooleanOpt3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x1==>x2==>x3==>a", response.getExecuteStepStr());
	}

	// IF情况下AND+OR+NOT混合复杂
	@Test
	public void testBooleanOpt4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x1==>x3==>x3==>x4==>a", response.getExecuteStepStr());
	}

	@Test
	public void testBooleanOpt5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("w1==>w2==>a==>bk==>w1==>w2==>a==>bk==>w1==>w2==>a==>bk==>w1==>w2==>a==>bk", response.getExecuteStepStr());
	}

	// AND + NOT 实现短路效果
	@Test
	public void testBooleanOpt6() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x1==>b", response.getExecuteStepStr());
	}

	// 有个布尔组件isAccess为false的情况
	@Test
	public void testBooleanOpt7() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x3==>x4==>b", response.getExecuteStepStr());
	}
}
