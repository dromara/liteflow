package com.yomahub.liteflow.test.parsecustom;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
 * springboot环境的自定义yml parser单元测试 主要测试自定义配置源类是否能引入springboot中的其他依赖
 *
 * @author junjun
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/parsecustom/application-custom-yml.properties")
@SpringBootTest(classes = CustomParserYmlELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.parsecustom.cmp", "com.yomahub.liteflow.test.parsecustom.bean" })
public class CustomParserYmlELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	// 测试springboot场景的自定义json parser
	@Test
	public void testYmlCustomParser() {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
		Assert.assertTrue(response.isSuccess());
	}

}
