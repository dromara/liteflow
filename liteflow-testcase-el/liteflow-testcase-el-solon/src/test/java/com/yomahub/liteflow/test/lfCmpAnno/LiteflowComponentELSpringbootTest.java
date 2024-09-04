package com.yomahub.liteflow.test.lfCmpAnno;

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
 * 测试@LiteflowComponent标注
 *
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@SolonTest
@Import(profiles="classpath:/lfCmpAnno/application.properties")
public class LiteflowComponentELSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testConfig() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a[A组件]==>b[B组件]==>c[C组件]==>b[B组件]==>a[A组件]==>d", response.getExecuteStepStr());
	}

}
