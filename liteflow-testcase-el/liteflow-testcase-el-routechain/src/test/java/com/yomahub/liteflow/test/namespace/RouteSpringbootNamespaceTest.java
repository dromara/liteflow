package com.yomahub.liteflow.test.namespace;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoMatchedRouteChainException;
import com.yomahub.liteflow.exception.RouteChainNotFoundException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
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
@TestPropertySource(value = "classpath:/namespace/application.properties")
@SpringBootTest(classes = RouteSpringbootNamespaceTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.namespace.cmp" })
public class RouteSpringbootNamespaceTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// n1 space中的两个链路都能匹配
	@Test
	public void testNamespaceRoute1() throws Exception {
		List<LiteflowResponse> responseList = flowExecutor.executeRouteChain("n1", 15, DefaultContext.class);
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

	// n1这个namespace中没有规则被匹配上
	@Test
	public void testNamespaceRoute2() throws Exception {
		Assertions.assertThrows(NoMatchedRouteChainException.class, () -> flowExecutor.executeRouteChain("n1", 8, DefaultContext.class));
	}

	// 没有n3这个namespace
	@Test
	public void testNamespaceRoute3() throws Exception {
		Assertions.assertThrows(RouteChainNotFoundException.class, () -> flowExecutor.executeRouteChain("n3", 8, DefaultContext.class));
	}
}
