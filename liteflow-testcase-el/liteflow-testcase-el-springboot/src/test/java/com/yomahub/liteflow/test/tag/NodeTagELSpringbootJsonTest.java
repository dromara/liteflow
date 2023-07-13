package com.yomahub.liteflow.test.tag;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境下隐私投递的测试
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@TestPropertySource(value = "classpath:/tag/application-json.properties")
@SpringBootTest(classes = NodeTagELSpringbootJsonTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.tag.cmp" })
public class NodeTagELSpringbootJsonTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testTag1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("123", context.getData("test"));
	}

	@Test
	public void testTag2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>a==>a==>c==>e", response.getExecuteStepStr());
	}

	// 测试多线程when情况下的tag取值是否正确
	// 这里循环多次的原因是，因为when多线程，有时候因为凑巧，可能正确。所以多次情况下在2.6.4版本肯定出错
	@Test
	public void testTag3() throws Exception {
		for (int i = 0; i < 50; i++) {
			LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
			DefaultContext context = response.getFirstContextBean();
			Assertions.assertTrue(response.isSuccess());
			ConcurrentHashSet<String> testSet = context.getData("test");
			Assertions.assertEquals(3, testSet.size());
		}
	}

	// 测试tag是否能在isAccess中起效
	@Test
	public void testTag4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("g", response.getExecuteStepStr());
	}

}
