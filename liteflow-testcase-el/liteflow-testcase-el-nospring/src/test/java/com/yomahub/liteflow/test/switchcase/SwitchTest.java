package com.yomahub.liteflow.test.switchcase;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class SwitchTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeClass
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("switchcase/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testSwitch1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>e==>d==>b", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>e==>d", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch3() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>f==>b", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch4() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>g==>d", response.getExecuteStepStr());
	}

	@Test
	public void testSwitch5() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("a==>h==>b", response.getExecuteStepStr());
	}

}
