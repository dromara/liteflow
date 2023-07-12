package com.yomahub.liteflow.test.refreshRule;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 非spring环境下重新加载规则测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class RefreshRuleTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("refreshRule/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 测试普通刷新流程的场景
	@Test
	public void testRefresh1() throws Exception {
		String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.el.xml");
		FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, content);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

	// 测试优雅刷新的场景
	@Test
	public void testRefresh2() throws Exception {
		new Thread(() -> {
			try {
				Thread.sleep(4000L);
				String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.el.xml");
				FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, content);
			}
			catch (Exception e) {
				e.printStackTrace();
			}

		}).start();

		for (int i = 0; i < 500; i++) {
			LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
			Assertions.assertTrue(response.isSuccess());
			try {
				Thread.sleep(10L);
			}
			catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

	}

}
