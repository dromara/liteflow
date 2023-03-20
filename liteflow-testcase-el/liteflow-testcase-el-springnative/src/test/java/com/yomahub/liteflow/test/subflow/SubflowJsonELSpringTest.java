package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 测试显示调用子流程(json) 单元测试
 *
 * @author justin.xu
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/subflow/application-json.xml")
public class SubflowJsonELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 是否按照流程定义配置执行
	@Test
	public void testExplicitSubFlow() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getExecuteStepStr());
	}

}
