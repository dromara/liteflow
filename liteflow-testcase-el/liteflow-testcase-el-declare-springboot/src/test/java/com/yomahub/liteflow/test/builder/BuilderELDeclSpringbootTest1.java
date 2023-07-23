package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.base.BaseELDeclSpringbootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

//基于builder模式的单元测试
//这里只是最基本的builder模式的测试，只是为了验证在springboot模式下的正常性
//更详细的builder模式测试用例会单独拉testcase去做
@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = BuilderELDeclSpringbootTest1.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.builder.cmp1" })
public class BuilderELDeclSpringbootTest1 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 基于普通组件的builder模式测试
	@Test
	public void testBuilder() throws Exception {
		LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL("THEN(c, d)").build();

		LiteFlowChainELBuilder.createChain()
			.setChainName("chain1")
			.setEL("THEN(a, b, WHEN(SWITCH(e).to(f, g, chain2)))")
			.build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>e==>c==>d", response.getExecuteStepStrWithoutTime());
	}

	// 基于普通组件的builder模式测试
	@Test
	public void testBuilderForConditionNode() throws Exception {
		LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL("THEN(c, d)").build();

		LiteFlowChainELBuilder.createChain()
			.setChainName("chain1")
			.setEL("THEN(a.tag('hello'), b, WHEN(SWITCH(e).to(f.tag('FHello'), g, chain2)))")
			.build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>e==>c==>d", response.getExecuteStepStr());
	}

}
