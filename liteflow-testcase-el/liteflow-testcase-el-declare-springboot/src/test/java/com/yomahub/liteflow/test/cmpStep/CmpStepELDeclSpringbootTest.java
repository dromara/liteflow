package com.yomahub.liteflow.test.cmpStep;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.entity.CmpStep;
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
import java.util.*;

/**
 * springboot环境最普通的例子测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/cmpStep/application.properties")
@SpringBootTest(classes = CmpStepELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.cmpStep.cmp" })
public class CmpStepELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testStep() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertFalse(response.isSuccess());
		Assert.assertTrue(response.getExecuteSteps().get("a").get(0).isSuccess());
		Assert.assertTrue(response.getExecuteSteps().get("b").get(0).isSuccess());
		Assert.assertFalse(response.getExecuteSteps().get("c").get(0).isSuccess());
		Assert.assertFalse(response.getExecuteSteps().get("d").get(0).isSuccess());
		Assert.assertTrue(response.getExecuteSteps().get("c").get(0).getTimeSpent() >= 2000);
		Assert.assertEquals(RuntimeException.class, response.getExecuteSteps().get("c").get(0).getException().getClass());
		Assert.assertEquals(RuntimeException.class, response.getExecuteSteps().get("d").get(0).getException().getClass());
	}

	@Test
	public void testStep2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>b", response.getExecuteStepStrWithoutTime());
	}

	@Test
	public void testStep3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assert.assertTrue(response.isSuccess());
		Map<String, List<CmpStep>> stepMap = response.getExecuteSteps();
		Assert.assertEquals(2, stepMap.size());
		Queue<CmpStep> queue = response.getExecuteStepQueue();
		Assert.assertEquals(5, queue.size());

		Set<String> tagSet = new HashSet<>();
		response.getExecuteStepQueue()
			.stream()
			.filter(cmpStep -> cmpStep.getNodeId().equals("a"))
			.forEach(cmpStep -> tagSet.add(cmpStep.getTag()));

		Assert.assertEquals(3, tagSet.size());

	}

}
