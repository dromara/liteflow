package com.yomahub.liteflow.test.complex;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境EL复杂例子测试1
 *
 * @author Bryan.Zhang
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/complex/application1.properties")
@SpringBootTest(classes = ComplexELDeclMultiSpringbootTest1.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.complex.cmp1" })
public class ComplexELDeclMultiSpringbootTest1 extends BaseTest {

	@Resource
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
