package com.yomahub.liteflow.test.customNodes;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * solon环境下自定义声明节点的测试 不通过spring扫描的方式，通过在配置文件里定义nodes的方式
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@SolonTest
@Import(profiles="classpath:/customNodes/application.properties")
public class CustomNodesELSpringbootTest extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testCustomNodes() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
