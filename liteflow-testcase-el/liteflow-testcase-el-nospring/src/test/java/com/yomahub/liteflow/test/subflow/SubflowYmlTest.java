package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 测试显示调用子流程(yml) 单元测试
 *
 * @author justin.xu
 */
public class SubflowYmlTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("subflow/flow.el.yml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 是否按照流程定义配置执行
	@Test
	public void testExplicitSubFlowYml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getExecuteStepStr());
	}

}
