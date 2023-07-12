package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 流程执行异常 单元测试
 *
 * @author zendwang
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/exception/application1.xml")
public class Exception1ELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

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

	@Test
	public void testNoConditionInChainException() throws Exception {
		Assertions.assertThrows(FlowExecutorNotInitException.class, () -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("exception/flow-blank.el.xml");
			flowExecutor.reloadRule();
		});


	}

}
