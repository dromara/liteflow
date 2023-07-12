package com.yomahub.liteflow.test.substituteNode;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit5Extension;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境EL替补节点的测试
 *
 * @author Bryan.Zhang
 */
@ExtendWith(SolonJUnit5Extension.class)
@TestPropertySource("classpath:/substituteNode/application.properties")
public class SubstituteSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testSub1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 有替补节点
	@Test
	public void testSub2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 测试特殊命名的节点
	@Test
	public void testSub3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
