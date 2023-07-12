package com.yomahub.liteflow.test.multipleType;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 测试非spring下混合格式规则的场景
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
public class LiteflowMultipleTypeTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("multipleType/flow.el.xml,multipleType/flow.el.yml");
		config.setSupportMultipleType(true);
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testMultipleType() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>b==>a", response.getExecuteStepStr());
		response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
	}

}
