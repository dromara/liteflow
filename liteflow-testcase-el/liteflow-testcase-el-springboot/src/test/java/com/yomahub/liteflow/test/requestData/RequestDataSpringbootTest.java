package com.yomahub.liteflow.test.requestData;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 * @author luo yi
 */
@TestPropertySource(value = "classpath:/requestData/application.properties")
@SpringBootTest(classes = RequestDataSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.requestData.cmp" })
public class RequestDataSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况
	@Test
	public void testReqData1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "hello");
		Assertions.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        String arg = context.getData("arg");
        Assertions.assertEquals("hello", arg);
	}
}
