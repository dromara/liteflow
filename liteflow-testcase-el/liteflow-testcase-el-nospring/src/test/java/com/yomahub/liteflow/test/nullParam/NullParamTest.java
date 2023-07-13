package com.yomahub.liteflow.test.nullParam;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 单元测试:传递null param导致NPE的优化代码
 *
 * @author LeoLee
 * @since 2.6.6
 */
public class NullParamTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("nullParam/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	/**
	 * 支持无参的flow执行，以及param 为null时的异常抛出
	 */
	@Test
	public void testNullParam() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1");
		Assertions.assertTrue(response.isSuccess());
	}

}
