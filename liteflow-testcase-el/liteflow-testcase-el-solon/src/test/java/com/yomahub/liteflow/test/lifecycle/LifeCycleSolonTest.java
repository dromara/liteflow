package com.yomahub.liteflow.test.lifecycle;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 生命周期例子
 *
 * @author Bryan.Zhang
 */
@SolonTest
@Import(profiles="classpath:/lifecycle/application.properties")
public class LifeCycleSolonTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testLifeCycle() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}
}
