package com.yomahub.liteflow.test.requestId;

import com.yomahub.liteflow.core.FlowExecutor;
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
 * @author tangkc
 */
@TestPropertySource(value = "classpath:/requestId/application.properties")
@SpringBootTest(classes = LiteflowRequestIdELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.requestId.cmp" })
public class LiteflowRequestIdELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testRequestId1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("1", response.getRequestId());
	}

	@Test
	public void testRequestId2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2RespWithRid("chain1", null, "T001234", DefaultContext.class);
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("T001234", response.getRequestId());
	}
}
