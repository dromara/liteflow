package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 非spring下的yml parser测试用例
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
public class YmlParserTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("parser/flow.el.yml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 测试无spring场景的yml parser
	@Test
	public void testYmlParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	@Test
	public void testYmlDisableParser() {
		Assertions.assertThrows(ChainNotFoundException.class,()->{
			throw flowExecutor.execute2Resp("chain3", "arg").getCause();
		});
	}
}
