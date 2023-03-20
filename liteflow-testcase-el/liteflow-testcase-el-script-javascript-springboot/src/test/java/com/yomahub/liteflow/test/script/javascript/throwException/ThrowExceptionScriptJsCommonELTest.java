package com.yomahub.liteflow.test.script.javascript.throwException;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
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
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/throwException/application.properties")
@SpringBootTest(classes = ThrowExceptionScriptJsCommonELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javascript.throwException.cmp" })
public class ThrowExceptionScriptJsCommonELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试普通脚本节点
	@Test
	public void testCommon1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		DefaultContext context = response.getFirstContextBean();
		Assert.assertFalse(response.isSuccess());
	}

}
