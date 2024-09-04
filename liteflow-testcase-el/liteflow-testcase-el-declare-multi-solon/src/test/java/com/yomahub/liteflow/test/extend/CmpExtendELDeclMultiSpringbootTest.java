package com.yomahub.liteflow.test.extend;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * springboot环境测试声明式组件继承其他类的场景
 *
 * @author Bryan.Zhang
 * @since 2.7.1
 */
@Import(profiles = "classpath:/extend/application.properties")
@SolonTest
public class CmpExtendELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testExtend() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
