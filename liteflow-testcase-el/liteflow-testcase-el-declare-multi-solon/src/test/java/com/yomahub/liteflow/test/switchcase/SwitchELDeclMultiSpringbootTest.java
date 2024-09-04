package com.yomahub.liteflow.test.switchcase;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

/**
 * springboot环境最普通的例子测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@Import(profiles ="classpath:/switchcase/application.properties")
@SolonTest
public class SwitchELDeclMultiSpringbootTest extends BaseTest {

	@Inject
	private FlowExecutor flowExecutor;

	@Test
	public void testSwitch1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>e==>d==>b", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>e==>d", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>f==>b", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>g==>d", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("a==>h==>b", response.getExecuteStepStr());
	}

}
