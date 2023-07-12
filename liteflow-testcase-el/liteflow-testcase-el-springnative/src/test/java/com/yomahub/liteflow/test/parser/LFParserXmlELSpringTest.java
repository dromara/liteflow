package com.yomahub.liteflow.test.parser;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import javax.annotation.Resource;

/**
 * spring环境的xml parser单元测试
 *
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:/parser/application-xml.xml")
public class LFParserXmlELSpringTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试spring场景的xml parser
	@Test
	public void testXmlParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
