package com.yomahub.liteflow.test.customMethodName;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 声明式组件自定义方法的测试用例
 *
 * @author Bryan.Zhang
 * @since 2.7.2
 */
@Import(profiles ="classpath:/customMethodName/application.properties")
@SolonTest
public class CustomMethodNameELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testCustomMethodName() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
