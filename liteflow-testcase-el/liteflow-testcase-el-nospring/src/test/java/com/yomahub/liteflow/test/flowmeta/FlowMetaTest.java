package com.yomahub.liteflow.test.flowmeta;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.enums.ParseModeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.flowmeta.cmp2.DCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class FlowMetaTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("flowmeta/flow.el.xml");
		config.setParseMode(ParseModeEnum.PARSE_ALL_ON_FIRST_EXEC);
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 测试动态添加元信息节点
	@Test
	public void testFlowMeta() {
		FlowBus.addNode("d", "d组件", NodeTypeEnum.COMMON, DCmp.class);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>d[d组件]", response.getExecuteStepStr());
	}

}
