package com.yomahub.liteflow.test.getChainName;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import javax.annotation.Resource;

/**
 * springboot环境获取ChainName的测试
 *
 * @author Bryan.Zhang
 */
@TestPropertySource(value = "classpath:/getChainName/application.properties")
@SpringBootTest(classes = GetChainNameELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.getChainName.cmp" })
public class GetChainNameELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testGetChainName1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("sub1", context.getData("a"));
		Assertions.assertEquals("sub2", context.getData("b"));
		Assertions.assertEquals("sub3", context.getData("c"));
		Assertions.assertEquals("sub4", context.getData("d"));
	}

	@Test
	public void testGetChainName2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("chain2", context.getData("g"));
		Assertions.assertEquals("sub1", context.getData("a"));
		Assertions.assertEquals("sub2", context.getData("b"));
		Assertions.assertEquals("sub3", context.getData("c"));
		Assertions.assertEquals("sub4", context.getData("d"));
		Assertions.assertEquals("sub5", context.getData("f"));
		Assertions.assertEquals("sub5_chain2", context.getData("e"));
		Assertions.assertEquals("sub6", context.getData("h"));
		Assertions.assertEquals("sub6", context.getData("j"));
		Assertions.assertNull(context.getData("k"));
	}

}
