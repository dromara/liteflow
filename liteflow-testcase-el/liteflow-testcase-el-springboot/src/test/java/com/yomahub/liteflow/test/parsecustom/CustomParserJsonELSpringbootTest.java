package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境的自定义json parser单元测试
 *
 * @author dongguo.tao
 * @since 2.5.0
 */
@TestPropertySource(value = "classpath:/parsecustom/application-custom-json.properties")
@SpringBootTest(classes = CustomParserJsonELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.parsecustom.cmp" })
public class CustomParserJsonELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试springboot场景的自定义json parser
	@Test
	public void testJsonCustomParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
		Assertions.assertTrue(response.isSuccess());
	}

}
