package com.yomahub.liteflow.test.multipleType;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * 测试springboot下混合格式规则的场景
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@Import(profiles ="classpath:/multipleType/application.properties")
@SolonTest
public class LiteflowMultipleTypeELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testMultipleType() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c==>b==>a", response.getExecuteStepStr());
		response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
	}

}
