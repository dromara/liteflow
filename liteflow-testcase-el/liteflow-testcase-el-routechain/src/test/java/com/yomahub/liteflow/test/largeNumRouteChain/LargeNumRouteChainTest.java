package com.yomahub.liteflow.test.largeNumRouteChain;

import cn.hutool.core.date.StopWatch;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.NoMatchedRouteChainException;
import com.yomahub.liteflow.exception.RouteChainNotFoundException;
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
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/largeNumRouteChain/application.properties")
@SpringBootTest(classes = LargeNumRouteChainTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.largeNumRouteChain.cmp" })
public class LargeNumRouteChainTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 在大量的路由规则里只有3个匹配
	@Test
	public void test1() throws Exception {
		StopWatch sw = new StopWatch();
		sw.start();
		List<LiteflowResponse> responseList = flowExecutor.executeRouteChain("n1", 15, DefaultContext.class);

		List<LiteflowResponse> resultList = responseList.stream().filter(
                LiteflowResponse::isSuccess
		).collect(Collectors.toList());

		Assertions.assertEquals(3, resultList.size());

		sw.stop();
		System.out.println("耗时:" + sw.getTotalTimeMillis());
	}
}
