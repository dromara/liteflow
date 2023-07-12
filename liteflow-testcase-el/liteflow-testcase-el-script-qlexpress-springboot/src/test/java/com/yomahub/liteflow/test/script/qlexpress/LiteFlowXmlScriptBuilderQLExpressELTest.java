package com.yomahub.liteflow.test.script.qlexpress;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = LiteFlowXmlScriptBuilderQLExpressELTest.class)
@EnableAutoConfiguration
public class LiteFlowXmlScriptBuilderQLExpressELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试通过builder方式运行普通script节点，以脚本文本的方式运行
	@Test
	public void testBuilderScript1() {
		LiteFlowNodeBuilder.createNode()
			.setId("a")
			.setName("组件A")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.script.qlexpress.cmp.ACmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("b")
			.setName("组件B")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.script.qlexpress.cmp.BCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("c")
			.setName("组件C")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.script.qlexpress.cmp.CCmp")
			.build();
		LiteFlowNodeBuilder.createScriptNode()
			.setId("s1")
			.setName("普通脚本S1")
			.setType(NodeTypeEnum.SCRIPT)
			.setScript("a=3;b=2;defaultContext.setData(\"s1\",a*b);")
			.build();

		LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL("THEN(a,b,c,s1)").build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg1");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(Integer.valueOf(6), context.getData("s1"));
	}

	// 测试通过builder方式运行普通script节点，以file的方式运行
	@Test
	public void testBuilderScript2() {
		LiteFlowNodeBuilder.createNode()
			.setId("d")
			.setName("组件D")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.script.qlexpress.cmp.DCmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("s2")
			.setName("条件脚本S2")
			.setType(NodeTypeEnum.SWITCH_SCRIPT)
			.setFile("builder/s2.ql")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("a")
			.setName("组件A")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.script.qlexpress.cmp.ACmp")
			.build();
		LiteFlowNodeBuilder.createNode()
			.setId("b")
			.setName("组件B")
			.setType(NodeTypeEnum.COMMON)
			.setClazz("com.yomahub.liteflow.test.script.qlexpress.cmp.BCmp")
			.build();

		LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL("THEN(d,SWITCH(s2).to(a,b))").build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("d[组件D]==>s2[条件脚本S2]==>b[组件B]", response.getExecuteStepStr());
	}

}
