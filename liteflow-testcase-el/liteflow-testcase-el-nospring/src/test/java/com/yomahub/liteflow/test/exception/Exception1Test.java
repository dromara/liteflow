package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 流程执行异常 单元测试
 *
 * @author zendwang
 */
public class Exception1Test extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("exception/flow.el.xml");
		config.setWhenMaxWaitSeconds(1);
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	/**
	 * 验证 chain 节点重复的异常
	 */
	@Test
	public void testChainDuplicateException() {
		Assertions.assertThrows(ChainDuplicateException.class, () -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("exception/flow-exception.el.xml");
			flowExecutor.reloadRule();
		});
	}

	@Test
	public void testConfigErrorException() {
		Assertions.assertThrows(ConfigErrorException.class, () -> {
			flowExecutor.setLiteflowConfig(null);
			flowExecutor.reloadRule();
		});
	}

	@Test
	public void testFlowExecutorNotInitException() {
		Assertions.assertThrows(FlowExecutorNotInitException.class, () -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("error/flow.txt");
			flowExecutor.reloadRule();
		});
	}

}
