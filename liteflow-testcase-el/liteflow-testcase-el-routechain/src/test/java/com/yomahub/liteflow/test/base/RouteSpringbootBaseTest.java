package com.yomahub.liteflow.test.base;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoMatchedRouteChainException;
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
import java.util.List;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/base/application.properties")
@SpringBootTest(classes = RouteSpringbootBaseTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.base.cmp" })
public class RouteSpringbootBaseTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 最简单的情况，两个都满足
	@Test
	public void testBaseRoute1() throws Exception {
		List<LiteflowResponse> responseList = flowExecutor.executeRouteChain(15, DefaultContext.class);
		LiteflowResponse response1 = responseList.stream().filter(
				liteflowResponse -> liteflowResponse.getChainId().equals("r_chain1")
		).findFirst().orElse(null);

        assert response1 != null;
        Assertions.assertTrue(response1.isSuccess());
		Assertions.assertEquals("b==>a", response1.getExecuteStepStr());

		LiteflowResponse response2 = responseList.stream().filter(
				liteflowResponse -> liteflowResponse.getChainId().equals("r_chain2")
		).findFirst().orElse(null);

		assert response2 != null;
		Assertions.assertTrue(response2.isSuccess());
		Assertions.assertEquals("a==>b", response2.getExecuteStepStr());
	}
}
