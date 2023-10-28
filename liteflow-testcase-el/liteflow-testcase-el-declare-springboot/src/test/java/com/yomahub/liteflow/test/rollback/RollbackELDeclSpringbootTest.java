package com.yomahub.liteflow.test.rollback;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.whenTimeOut.WhenTimeOutELDeclSpringbootTest1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;


@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/rollback/application.properties")
@SpringBootTest(classes = RollbackELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.rollback.cmp" })
public class RollbackELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 在流程正常执行结束情况下的测试
	@Test
	public void testRollback() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertNull(response.getCause());
		Assertions.assertEquals("", response.getRollbackStepStr());
	}

	// 对串行编排与并行编排语法的测试
	@Test
	public void testWhenAndThen() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("d==>b==>a", response.getRollbackStepStr());
	}

	// 对条件编排语法的测试
	@Test
	public void testIf() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("d==>x", response.getRollbackStepStr());
	}

	// 对选择编排语法的测试
	@Test
	public void testSwitch() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("d==>f", response.getRollbackStepStr());
	}

	// 对FOR循环编排语法的测试
	@Test
	public void testFor() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("h==>b==>g", response.getRollbackStepStr());
	}

	// 对WHILE循环编排语法的测试
	@Test
	public void testWhile() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("d==>b==>a==>w", response.getRollbackStepStr());
	}

	// 对ITERATOR迭代循环编排语法的测试
	@Test
	public void testIterator() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("d==>b==>a==>i", response.getRollbackStepStr());
	}

	@Test
	// 对捕获异常表达式的测试
	public void testCatch() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain8", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertNull(response.getCause());
		Assertions.assertEquals("", response.getRollbackStepStr());
	}

	@Test
	// 对获取数据的测试
	public void testData() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain9", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("321", context.getData("test"));
	}

	@Test
	// 对重试的测试
	public void testRetry() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain10", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("n==>m", response.getRollbackStepStr());
	}

}
