package com.yomahub.liteflow.test.absoluteConfigPath;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

/**
 * springboot环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@TestPropertySource(value = "classpath:/absoluteConfigPath/application.properties")
@SpringBootTest(classes = AbsoluteConfigPathELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.absoluteConfigPath.cmp" })
public class AbsoluteConfigPathELSpringbootTest extends BaseTest {

	private final Logger log = LoggerFactory.getLogger(this.getClass());

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testAbsoluteConfig() throws Exception {
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertTrue(response.isSuccess());
	}

}
