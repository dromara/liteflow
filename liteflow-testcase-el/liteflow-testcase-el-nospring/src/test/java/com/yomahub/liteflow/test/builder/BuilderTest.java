package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.builder.cmp.*;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class BuilderTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 基于普通组件的builder模式测试
	@Test
	public void testBuilder() throws Exception {
		LiteFlowNodeBuilder.createNode()
			.setId("a")
			.setName("组件A")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.ACmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("b")
			.setName("组件B")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.BCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c")
			.setName("组件C")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.CCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("d")
			.setName("组件D")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.DCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("e")
			.setName("组件E")
			.setType(NodeTypeEnum.SWITCH)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.ECmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("f")
			.setName("组件F")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.FCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("g")
			.setName("组件G")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp.GCmp")
			.build();

		LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL("THEN(c, d)").build();

		LiteFlowChainELBuilder.createChain()
			.setChainName("chain1")
			.setEL("THEN(a, b, WHEN(SWITCH(e).to(f, g, chain2)))")
			.build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a[组件A]==>b[组件B]==>e[组件E]==>c[组件C]==>d[组件D]", response.getExecuteStepStr());
	}

	// 基于普通组件的builder模式测试
	@Test
	public void testBuilderForClassAndCode() throws Exception {
		LiteFlowNodeBuilder.createNode()
			.setId("a")
			.setName("组件A")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(ACmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("b")
			.setName("组件B")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(BCmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c")
			.setName("组件C")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(CCmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("d")
			.setName("组件D")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(DCmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("e")
			.setName("组件E")
			.setType(NodeTypeEnum.SWITCH)
			.setClazz(ECmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("f")
			.setName("组件F")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(FCmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("g")
			.setName("组件G")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(GCmp.class)
			.build();

		LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL("THEN(c, d)").build();

		LiteFlowChainELBuilder.createChain()
			.setChainName("chain1")
			.setEL("THEN(a, b, WHEN(SWITCH(e).to(f, g, chain2)))")
			.build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a[组件A]==>b[组件B]==>e[组件E]==>c[组件C]==>d[组件D]", response.getExecuteStepStr());
	}

	// 基于普通组件的builder模式测试
	@Test
	public void testBuilderForSameNodeMultiTimes() throws Exception {
		LiteFlowNodeBuilder.createNode()
			.setId("a1")
			.setName("组件A1")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(ACmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("a2")
			.setName("组件A2")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(ACmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c1")
			.setName("组件C1")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(CCmp.class)
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c2")
			.setName("组件C2")
			.setType(NodeTypeEnum.COMMON)
			.setClazz(CCmp.class)
			.build();

		LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL("THEN(a1,c2,a2,c1)").build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a1[组件A1]==>c2[组件C2]==>a2[组件A2]==>c1[组件C1]", response.getExecuteStepStr());
	}

}
