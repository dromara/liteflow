package com.yomahub.liteflow.test.complex.test1;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * springboot环境EL复杂例子测试1
 *
 * @author Bryan.Zhang
 */
@Import(profiles ="classpath:/complex/application1.properties")
@SolonTest
public class ComplexELDeclMultiSpringbootTest1 extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 测试复杂例子，优化前
	// 案例来自于文档中 EL规则写法/复杂编排例子/复杂例子一
	// 因为所有的组件都是空执行，你可以在组件里加上Thread.sleep来模拟业务耗时，再来看这个打出结果
	@Test
	public void testComplex1_1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1_1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 测试复杂例子，优化后
	// 案例来自于文档中 EL规则写法/复杂编排例子/复杂例子一
	// 因为所有的组件都是空执行，你可以在组件里加上Thread.sleep来模拟业务耗时，再来看这个打出结果
	@Test
	public void testComplex1_2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1_2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
