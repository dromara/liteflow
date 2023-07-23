package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot下的yml parser测试用例
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@TestPropertySource(value = "classpath:/parser/application-yml.properties")
@SpringBootTest(classes = YmlParserELSpringbootTest.class)
@EnableAutoConfiguration
public class YmlParserELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试无springboot场景的yml parser
	@Test
	public void testYmlParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
