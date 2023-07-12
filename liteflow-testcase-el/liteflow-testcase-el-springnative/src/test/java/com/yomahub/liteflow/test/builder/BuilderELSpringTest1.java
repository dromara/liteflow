package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.builder.cmp1.ACmp;
import com.yomahub.liteflow.test.builder.cmp1.BCmp;
import com.yomahub.liteflow.test.builder.cmp1.CCmp;
import com.yomahub.liteflow.test.builder.cmp1.DCmp;
import com.yomahub.liteflow.test.builder.cmp1.ECmp;
import com.yomahub.liteflow.test.builder.cmp1.FCmp;
import com.yomahub.liteflow.test.builder.cmp1.GCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

//基于builder模式的单元测试
//这里只是最基本的builder模式的测试，只是为了验证在spring模式下的正常性
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/builder/application1.xml")
public class BuilderELSpringTest1 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 基于普通组件的builder模式测试
	@Test
	public void testBuilder() throws Exception {
		LiteFlowNodeBuilder.createNode()
			.setId("a")
			.setName("组件A")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.ACmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("b")
			.setName("组件B")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.BCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c")
			.setName("组件C")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.CCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("d")
			.setName("组件D")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.DCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("e")
			.setName("组件E")
			.setType(NodeTypeEnum.SWITCH)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.ECmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("f")
			.setName("组件F")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.FCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("g")
			.setName("组件G")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.builder.cmp1.GCmp")
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

}
