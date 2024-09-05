package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.aop.aspect.CmpAspect;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 切面场景单元测试
 *
 * @author Bryan.Zhang
 */
@Import(profiles =  "classpath:/aop/application.properties", classes = CmpAspect.class)
@SolonTest
public class GlobalAOPELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 测试全局AOP，串行场景
	@Test
	public void testGlobalAopS() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
		Assertions.assertEquals("before_after", context.getData("d"));
		Assertions.assertEquals("before_after", context.getData("e"));
	}

	// 测试全局AOP，并行场景
	@Test
	public void testGlobalAopP() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
		Assertions.assertEquals("before_after", context.getData("d"));
		Assertions.assertEquals("before_after", context.getData("e"));
	}

	@Test
	public void testGlobalAopException() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "it's a request");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("before_after", context.getData("a"));
		Assertions.assertEquals("before_after", context.getData("b"));
		Assertions.assertEquals("before_after", context.getData("c"));
		Assertions.assertEquals("before_after", context.getData("f"));
		Assertions.assertEquals("test error", context.getData("f_error"));
	}

	@AfterAll
	public static void cleanScanCache() {
		BaseTest.cleanScanCache();
		//ComponentScanner.cmpAroundAspect = null;
	}

}
