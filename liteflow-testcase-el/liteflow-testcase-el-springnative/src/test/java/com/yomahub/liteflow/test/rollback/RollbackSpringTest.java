package com.yomahub.liteflow.test.rollback;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;


@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/rollback/application.xml")
public class RollbackSpringTest extends BaseTest {

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

	// 测试产生异常之后的回滚顺序
	@Test
	public void testRollbackStep() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("d==>b==>a", response.getRollbackStepStr());
	}


}
