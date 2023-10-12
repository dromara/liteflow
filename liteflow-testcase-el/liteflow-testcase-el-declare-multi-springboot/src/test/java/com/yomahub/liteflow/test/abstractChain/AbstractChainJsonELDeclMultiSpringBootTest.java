package com.yomahub.liteflow.test.abstractChain;

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
 * 测试显示调用子流程(json) 单元测试
 *
 * @author justin.xu
 */
@TestPropertySource(value = "classpath:/abstractChain/application-json.properties")
@SpringBootTest(classes = AbstractChainJsonELDeclMultiSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.abstractChain.cmp" })
public class AbstractChainJsonELDeclMultiSpringBootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 是否按照流程定义配置执行
	@Test
	public void test1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("implA", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>d==>f==>j", response.getExecuteStepStrWithoutTime());
	}

	//测试嵌套继承的baseChain是否重复解析
	@Test
	public void test2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("implB", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>a==>b==>a==>b==>f==>j", response.getExecuteStepStrWithoutTime());
	}

	//测试嵌套继承的baseChain是否重复解析
	@Test
	public void test3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("implC", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>a==>b==>a==>b==>f==>a==>b", response.getExecuteStepStrWithoutTime());
	}

	@Test
	public void test4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("implD", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>a==>b==>j==>k==>c==>d==>f==>j", response.getExecuteStepStrWithoutTime());
	}

}
