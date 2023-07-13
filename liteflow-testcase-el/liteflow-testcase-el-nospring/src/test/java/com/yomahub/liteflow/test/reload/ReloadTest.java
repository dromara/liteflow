package com.yomahub.liteflow.test.reload;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
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
 * @since 2.5.0
 */
public class ReloadTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeAll
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("reload/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	// 用reloadRule去重新加载，这里如果配置是放在本地。如果想修改，则要去修改target下面的flow.xml
	// 这里的测试，手动打断点然后去修改，是ok的。但是整个测试，暂且只是为了测试这个功能是否能正常运行
	@Test
	public void testReload() throws Exception {
		flowExecutor.reloadRule();
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
