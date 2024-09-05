package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 测试显示调用子流程(yml) 单元测试
 *
 * @author justin.xu
 */
@Import(profiles ="classpath:/subflow/application-yml.properties")
@SolonTest
public class SubflowYmlELDeclMultiSpringBootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 是否按照流程定义配置执行
	@Test
	public void testExplicitSubFlowYml() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getExecuteStepStr());
	}

}
