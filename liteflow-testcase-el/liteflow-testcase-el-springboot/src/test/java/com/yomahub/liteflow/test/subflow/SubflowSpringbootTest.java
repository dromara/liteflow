package com.yomahub.liteflow.test.subflow;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.subflow.context.TestContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.Map;
import java.util.Set;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/subflow/application.properties")
@SpringBootTest(classes = SubflowSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.subflow.cmp" })
public class SubflowSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	//测试子变量形式
	@Test
	public void testSubflow1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertTrue(ListUtil.toList("a==>c==>d==>b","a==>c==>b==>d").contains(response.getExecuteStepStr()));
	}

	//测试子chain
	@Test
	public void testSubflow2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertTrue(ListUtil.toList("a==>c==>d==>b","a==>c==>b==>d").contains(response.getExecuteStepStr()));
	}

	//测试在组件里调用另一个流程
	@Test
	public void testSubflow3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());

		DefaultContext context = response.getFirstContextBean();
		Set<String> set = context.getData("set");
		Assertions.assertEquals(100, set.size());
	}

	@Test
	public void testSubflow4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());

		DefaultContext context = response.getFirstContextBean();
		Map<String, Integer> testMap = context.getData("testMap");
		Assertions.assertEquals(100, testMap.get("out"));
		Assertions.assertEquals(100, testMap.get("inner"));
	}

	@Test
	public void testSubflow7() throws Exception {
		for (int i = 0; i < 500; i++) {
			LiteflowResponse response = flowExecutor.execute2Resp("chain7", null, TestContext.class);
			Assertions.assertTrue(response.isSuccess());
			TestContext context = response.getFirstContextBean();
			Assertions.assertEquals(5, context.getSet().size());
		}
	}

}
