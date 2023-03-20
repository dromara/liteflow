package com.yomahub.liteflow.test.script.graaljs.ifelse;

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

@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/ifelse/application.properties")
@SpringBootTest(classes = LiteFlowXmlScriptIfelseJsELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.graaljs.ifelse.cmp" })
public class LiteFlowXmlScriptIfelseJsELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// IF只有2个参数
	@Test
	public void testIf1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x0==>a==>b", response.getExecuteStepStrWithoutTime());
	}

	// IF只有3个参数
	@Test
	public void testIf2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>c==>d", response.getExecuteStepStrWithoutTime());
	}

	// IF有3个参数，进行嵌套
	@Test
	public void testIf3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x1==>c==>c==>b", response.getExecuteStepStrWithoutTime());
	}

	// IF有2个参数，加上ELSE
	@Test
	public void testIf4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>c==>d", response.getExecuteStepStrWithoutTime());
	}

	// IF有2个参数，ELSE里再嵌套一个IF
	@Test
	public void testIf5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x1==>c==>c==>b", response.getExecuteStepStrWithoutTime());
	}

	// 标准的IF ELIF ELSE
	@Test
	public void testIf6() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x0==>c==>c", response.getExecuteStepStrWithoutTime());
	}

	// IF ELIF... ELSE 的形式
	@Test
	public void testIf7() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("x1==>x1==>x1==>x1==>d==>b==>a", response.getExecuteStepStrWithoutTime());
	}

}
