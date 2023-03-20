package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

//基于builder模式的单元测试
//这里测试的是通过spring去扫描，但是通过代码去构建chain的用例
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/builder/application2.xml")
public class BuilderELSpringTest2 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 通过spring去扫描组件，通过代码去构建chain
	@Test
	public void testBuilder() throws Exception {
		LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL("THEN(h, i, j)").build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("h==>i==>j", response.getExecuteStepStr());
	}

}
