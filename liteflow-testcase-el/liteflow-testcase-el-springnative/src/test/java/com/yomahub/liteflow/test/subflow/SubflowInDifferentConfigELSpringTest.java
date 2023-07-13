package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.MultipleParsersException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * 测试主流程与子流程在不同的配置文件的场景
 *
 * @author Bryan.Zhang
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/subflow/application-subInDifferentConfig1.xml")
public class SubflowInDifferentConfigELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 是否按照流程定义配置执行
	@Test
	public void testExplicitSubFlow1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>b==>a==>e==>d", response.getExecuteStepStr());
	}

	// 主要测试有不同的配置类型后会不会报出既定的错误
	@Test
	public void testExplicitSubFlow2() {
		Assertions.assertThrows(MultipleParsersException.class, () -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("subflow/flow-main.el.xml,subflow/flow-sub1.el.xml,subflow/flow-sub2.el.yml");
			flowExecutor.reloadRule();
		});
	}

}
