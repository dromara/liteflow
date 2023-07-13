package com.yomahub.liteflow.test.removeChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境最普通的例子测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@TestPropertySource(value = "classpath:/removeChain/application.properties")
@SpringBootTest(classes = RemoveChainELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.removeChain.cmp" })
public class RemoveChainELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testRemoveChain() throws Exception {
		LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response1.isSuccess());
		FlowBus.removeChain("chain1");
		LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertFalse(response2.isSuccess());
	}

}
