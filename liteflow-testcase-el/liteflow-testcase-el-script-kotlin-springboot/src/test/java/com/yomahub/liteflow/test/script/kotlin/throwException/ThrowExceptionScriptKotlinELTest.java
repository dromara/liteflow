package com.yomahub.liteflow.test.script.kotlin.throwException;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.script.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

/**
 * 测试 springboot下的 Kotlin 脚本抛错
 */
@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/throwException/application.properties")
@SpringBootTest(classes = ThrowExceptionScriptKotlinELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.kotlin.throwException.cmp" })
public class ThrowExceptionScriptKotlinELTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void test1() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertFalse(response.isSuccess());
		Assertions.assertEquals("T01", response.getCode());
	}

}
