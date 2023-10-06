package com.yomahub.liteflow.test.absoluteConfigPath;

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
 * @since 2.6.11
 */
public class AbsoluteConfigPathTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("/usr/local/flow2.xml");
//		config.setRuleSource("/usr/**/*.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testAbsoluteConfig() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
