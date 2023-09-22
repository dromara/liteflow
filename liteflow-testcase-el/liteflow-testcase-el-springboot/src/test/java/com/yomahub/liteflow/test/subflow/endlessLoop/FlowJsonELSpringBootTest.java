package com.yomahub.liteflow.test.subflow.endlessLoop;

import com.yomahub.liteflow.core.FlowExecutor;
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
 * 测试 json 文件情况下 chain 死循环逻辑
 *
 * @author luo yi
 * @since 2.11.1
 */
@TestPropertySource(value = "classpath:/subflow/endlessLoop/application-json.properties")
@SpringBootTest(classes = FlowJsonELSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.subflow.cmp1" })
public class FlowJsonELSpringBootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试 chain 死循环
	@Test
	public void testChainEndlessLoop() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "it's a request");
		Assertions.assertFalse(response.isSuccess());
	}

}
