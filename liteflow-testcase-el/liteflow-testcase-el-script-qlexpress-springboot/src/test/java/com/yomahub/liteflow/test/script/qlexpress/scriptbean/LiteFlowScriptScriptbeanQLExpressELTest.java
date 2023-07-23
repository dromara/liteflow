package com.yomahub.liteflow.test.script.qlexpress.scriptbean;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/scriptbean/application.properties")
@SpringBootTest(classes = LiteFlowScriptScriptbeanQLExpressELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.qlexpress.scriptbean.cmp",
		"com.yomahub.liteflow.test.script.qlexpress.scriptbean.bean" })
public class LiteFlowScriptScriptbeanQLExpressELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testScriptBean1() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello", context.getData("demo"));
	}

	@Test
	public void testScriptBean2() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertEquals("hello,kobe", context.getData("demo"));
	}

}
