package com.yomahub.liteflow.test.getChainName;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * nospring环境获取ChainName的测试
 *
 * @author Bryan.Zhang
 */
public class GetChainNameELTest extends BaseTest {

	private static FlowExecutor flowExecutor;

	@BeforeClass
	public static void init() {
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("getChainName/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@Test
	public void testGetChainName1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("sub1", context.getData("a"));
		Assert.assertEquals("sub2", context.getData("b"));
		Assert.assertEquals("sub3", context.getData("c"));
		Assert.assertEquals("sub4", context.getData("d"));
	}

	@Test
	public void testGetChainName2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assert.assertTrue(response.isSuccess());
		Assert.assertEquals("chain2", context.getData("g"));
		Assert.assertEquals("sub1", context.getData("a"));
		Assert.assertEquals("sub2", context.getData("b"));
		Assert.assertEquals("sub3", context.getData("c"));
		Assert.assertEquals("sub4", context.getData("d"));
		Assert.assertEquals("sub5", context.getData("f"));
		Assert.assertEquals("sub5_chain2", context.getData("e"));
		Assert.assertEquals("sub6", context.getData("h"));
		Assert.assertEquals("sub6", context.getData("j"));
		Assert.assertNull(context.getData("k"));
	}

}
