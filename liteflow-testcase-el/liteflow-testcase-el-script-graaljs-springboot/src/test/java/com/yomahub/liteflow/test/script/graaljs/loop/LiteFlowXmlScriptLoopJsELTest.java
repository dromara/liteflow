package com.yomahub.liteflow.test.script.graaljs.loop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/loop/application.properties")
@SpringBootTest(classes = LiteFlowXmlScriptLoopJsELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.graaljs.loop.cmp" })
public class LiteFlowXmlScriptLoopJsELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// FOR循环数字直接在el中定义
	@Test
	public void testLoop1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("LOOP_2==>a==>b==>c==>a==>b==>c", response.getExecuteStepStr());
	}

	// FPR循环由For组件定义
	@Test
	public void testLoop2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("x==>a==>b==>c==>a==>b==>c==>a==>b==>c", response.getExecuteStepStr());
	}

	// FOR循环中加入BREAK组件
	@Test
	public void testLoop3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// WHILE循环
	@Test
	public void testLoop4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("z==>a==>d==>z==>a==>d==>z==>a==>d==>z==>a==>d==>z==>a==>d==>z",
				response.getExecuteStepStr());
	}

	// WHILE循环加入BREAK
	@Test
	public void testLoop5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("z==>a==>d==>y==>z==>a==>d==>y==>z==>a==>d==>y==>z==>a==>d==>y",
				response.getExecuteStepStr());
	}

}
