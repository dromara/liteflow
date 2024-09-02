package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * solon环境的自定义yml parser单元测试 主要测试自定义配置源类是否能引入solon中的其他依赖
 *
 * @author junjun
 */
@SolonTest
@Import(profiles="classpath:/parsecustom/application-custom-yml.properties")
public class CustomParserYmlELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	// 测试solon场景的自定义json parser
	@Test
	public void testYmlCustomParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
		Assertions.assertTrue(response.isSuccess());
	}

}
