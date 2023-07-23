package com.yomahub.liteflow.test.switchcase;

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
 * springboot环境EL常规的例子测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/switchcase/application.properties")
@SpringBootTest(classes = SwitchELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.switchcase.cmp" })
public class SwitchELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 2022-07-12 switch 异常错误.c.y.l.builder.el.operator.ToOperator : parameter error
	// run QlExpress Exception at line 1 :
	// switch().to(): 只有一个node时出错
	@Test
	public void testSwitch1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>e==>d==>b", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>e==>d", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>f==>b", response.getExecuteStepStr());
	}

	// 根据tag来跳转，指定哪个组件的tag
	@Test
	public void testSwitch4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>g==>d", response.getExecuteStepStr());
	}

	// tag的跳转
	@Test
	public void testSwitch5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>h==>b", response.getExecuteStepStr());
	}

	// 相同组件的tag的跳转
	@Test
	public void testSwitch6() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>h==>b", response.getExecuteStepStr());
	}

	// switch增加default选项
	@Test
	public void testSwitch7() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>i==>d", response.getExecuteStepStr());
	}

	// switch返回如果是空，会走default选项
	@Test
	public void testSwitch8() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain8", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>j==>d", response.getExecuteStepStr());
	}

	// 测试跳转到某个链路上
	@Test
	public void testSwitch9() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain9", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("k==>a==>b", response.getExecuteStepStr());
	}

	// 相同组件复用的用例
	@Test
	public void testSwitch10() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 表达式上设置tag
	@Test
	public void testSwitch11() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain12", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 给chain设置tag，跳转到chain上
	@Test
	public void testSwitch13() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain13", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
