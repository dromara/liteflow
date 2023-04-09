package com.yomahub.liteflow.test.comments;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
@TestPropertySource(value = "classpath:/comments/application.properties")
@SpringBootTest(classes = LiteflowNodeELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.comments.cmp" })
public class LiteflowNodeELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试注释
	@Test
	public void testComments1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a base request");
		DefaultContext context = response.getFirstContextBean();
		String str = context.getData("str");
		Assert.assertTrue(response.isSuccess());
		Assert.assertTrue(ListUtil.toList("a==>b==>c==>b", "a==>b==>b==>c").contains(response.getExecuteStepStr()));
		Assert.assertEquals("https://liteflow.yomahub.com", str);
	}

}