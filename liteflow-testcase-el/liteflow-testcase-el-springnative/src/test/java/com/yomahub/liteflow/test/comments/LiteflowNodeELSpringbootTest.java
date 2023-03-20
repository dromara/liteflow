package com.yomahub.liteflow.test.comments;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/comments/application.xml")
public class LiteflowNodeELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试注释
	@Test
	public void testAsyncFlow1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a base request");
		Assert.assertTrue(response.isSuccess());
		Assert.assertTrue(ListUtil.toList("a==>b==>c==>b", "a==>b==>b==>c").contains(response.getExecuteStepStr()));
	}

}