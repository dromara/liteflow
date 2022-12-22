package com.yomahub.liteflow.test.comments;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/comments/application.properties")
public class LiteflowNodeELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 测试注释
	@Test
	public void testAsyncFlow1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a base request");
		Assert.assertTrue(response.isSuccess());
		Assert.assertTrue(ListUtil.toList("a==>b==>c==>b","a==>b==>b==>c").contains(response.getExecuteStepStr()));
	}
}