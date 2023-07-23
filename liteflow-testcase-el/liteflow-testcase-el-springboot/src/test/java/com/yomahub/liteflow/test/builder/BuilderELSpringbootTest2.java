package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.Resource;

//基于builder模式的单元测试
//这里测试的是通过spring去扫描，但是通过代码去构建chain的用例
@SpringBootTest(classes = BuilderELSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.builder.cmp2" })
public class BuilderELSpringbootTest2 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 通过spring去扫描组件，通过代码去构建chain
	@Test
	public void testBuilder() throws Exception {
		LiteFlowChainELBuilder.createChain().setChainId("chain1").setEL("THEN(h, i, j)").build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("h==>i==>j", response.getExecuteStepStr());
	}

}
