package com.yomahub.liteflow.test.whenTimeOut;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 非spring环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class WhenTimeOutTest2 extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("whenTimeOut/flow2.el.xml");
		config.setWhenMaxWaitSeconds(5);
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 其中d,e,f都sleep 4秒，其中def是不同的组，超时设置5秒
	@Test
	public void testWhenTimeOut() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
