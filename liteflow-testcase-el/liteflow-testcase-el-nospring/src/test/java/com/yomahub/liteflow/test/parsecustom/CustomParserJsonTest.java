package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 非spring环境的自定义json parser单元测试
 *
 * @author dongguo.tao
 * @since 2.5.0
 */
public class CustomParserJsonTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("com.yomahub.liteflow.test.parsecustom.parser.CustomJsonFlowParser");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 测试非spring场景的自定义json parser
	@Test
	public void testJsonCustomParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
		Assertions.assertTrue(response.isSuccess());
	}

}
