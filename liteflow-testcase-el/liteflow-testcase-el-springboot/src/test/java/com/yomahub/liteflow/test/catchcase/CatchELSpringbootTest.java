package com.yomahub.liteflow.test.catchcase;

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
@TestPropertySource(value = "classpath:/catchcase/application.properties")
@SpringBootTest(classes = CatchELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.catchcase.cmp" })
public class CatchELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testCatch1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>c", response.getExecuteStepStrWithoutTime());
		Assert.assertNull(response.getCause());
	}

	@Test
	public void testCatch2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assert.assertFalse(response.isSuccess());
		Assert.assertEquals("a==>d", response.getExecuteStepStrWithoutTime());
	}

	@Test
	public void testCatch3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a", response.getExecuteStepStrWithoutTime());
	}

	@Test
	public void testCatch4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("LOOP_3==>a==>b==>a==>b==>a==>b", response.getExecuteStepStrWithoutTime());
	}

}
