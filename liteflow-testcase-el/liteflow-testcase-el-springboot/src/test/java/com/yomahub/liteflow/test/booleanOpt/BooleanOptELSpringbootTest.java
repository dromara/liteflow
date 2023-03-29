package com.yomahub.liteflow.test.booleanOpt;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
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
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x2==>x3==>b", response.getExecuteStepStr());
	}

	// IF情况下OR
	@Test
	public void testBooleanOpt2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x2==>x3==>a", response.getExecuteStepStr());
	}

	// IF情况下AND+NOT
	@Test
	public void testBooleanOpt3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x2==>x3==>a", response.getExecuteStepStr());
	}

	// IF情况下AND+OR+NOT混合复杂
	@Test
	public void testBooleanOpt4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x3==>x3==>x4==>a", response.getExecuteStepStr());
	}

	@Test
	public void testBooleanOpt5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("w1==>w2==>a==>bk==>w1==>w2==>a==>bk==>w1==>w2==>a==>bk==>w1==>w2==>a==>bk", response.getExecuteStepStr());
	}
}
