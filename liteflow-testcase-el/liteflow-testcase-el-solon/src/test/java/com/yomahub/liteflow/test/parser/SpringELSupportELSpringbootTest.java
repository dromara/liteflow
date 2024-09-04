package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

@SolonTest
@Import(profiles="classpath:/parser/application-springEL.properties")
public class SpringELSupportELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 测试springEL的解析情况
	@Test
	public void testSpringELParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
