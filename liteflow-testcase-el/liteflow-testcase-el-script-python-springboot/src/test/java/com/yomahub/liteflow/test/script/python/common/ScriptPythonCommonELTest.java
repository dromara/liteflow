package com.yomahub.liteflow.test.script.python.common;

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

/**
 * 测试springboot下的python脚本组件，基于xml配置
 *
 * @author Bryan.Zhang
 * @since 2.9.5
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/common/application.properties")
@SpringBootTest(classes = ScriptPythonCommonELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.python.common.cmp","com.yomahub.liteflow.test.script.python.common.domain" })
public class ScriptPythonCommonELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试普通脚本节点
	@Test
	public void testCommon1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assertions.assertTrue(response.isSuccess());
		Assertions.assertEquals(Integer.valueOf(30), context.getData("s1"));
		Assertions.assertEquals("jack", context.getData("name"));
		Assertions.assertEquals("hi,jack", context.getData("td"));
		Assertions.assertEquals("中文",context.getData("s2"));
	}

	@Test
	public void testCommon2() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
