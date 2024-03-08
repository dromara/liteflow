package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.aop.aspect.CustomOperatorAspect;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * 切面场景单元测试
 *
 * @author luo yi
 */
@TestPropertySource(value = "classpath:/aop/application.properties")
@SpringBootTest(classes = CustomAOPELOperatorSpringbootTest.class)
@EnableAutoConfiguration
@Import(CustomOperatorAspect.class)
@ComponentScan({ "com.yomahub.liteflow.test.aop.cmp1", "com.yomahub.liteflow.test.aop.cmp2" })
public class CustomAOPELOperatorSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 设置 isContinueOnError 测试全局 AOP，串行场景
	@Test
	public void testGlobalAopErrorWithContinueS() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
		Assertions.assertEquals("before", context.getData("f"));
	}

	// 设置 isContinueOnError 测试全局 AOP，并行场景
	@Test
	public void testGlobalAopErrorWithContinueP() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
		Assertions.assertEquals("before_after", context.getData("e"));
		Assertions.assertEquals("before", context.getData("f"));
	}

}
