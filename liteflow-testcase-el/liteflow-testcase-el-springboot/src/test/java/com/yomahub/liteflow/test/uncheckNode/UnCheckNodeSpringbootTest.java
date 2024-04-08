package com.yomahub.liteflow.test.uncheckNode;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.uncheckNode.cmp.XCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境不检查Node存在与否测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/uncheckNode/application.properties")
@SpringBootTest(classes = UnCheckNodeSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.uncheckNode.cmp" })
public class UnCheckNodeSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// X不存在,能启动就算成功
	@Test
	public void testUncheckNode1() throws Exception {

	}

	// X不存在，但是启动好加入进去，就可以
	@Test
	public void testUncheckNode2() throws Exception {
		LiteFlowNodeBuilder.createCommonNode().setId("x").setClazz(XCmp.class).build();

		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
	}
}
