package com.yomahub.liteflow.test.script.javascript.sw;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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

/**
 * 测试springboot下的groovy脚本组件，基于xml配置
 *
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/sw/application.properties")
@SpringBootTest(classes = LiteflowXmlScriptJsSwitchELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javascript.sw.cmp" })
public class LiteflowXmlScriptJsSwitchELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试选择脚本节点
	@Test
	public void testSw1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals("d==>s1[选择脚本]==>a", response.getExecuteStepStr());
	}

}
