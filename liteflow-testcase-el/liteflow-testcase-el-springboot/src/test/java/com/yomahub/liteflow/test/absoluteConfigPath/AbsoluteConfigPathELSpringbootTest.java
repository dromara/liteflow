package com.yomahub.liteflow.test.absoluteConfigPath;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
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
@SpringBootTest(classes = AbsoluteConfigPathELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.absoluteConfigPath.cmp" })
public class AbsoluteConfigPathELSpringbootTest extends BaseTest {


	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testAbsoluteConfig() throws Exception {
		Assertions.assertTrue(() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("C:/LiteFlow/Test/a/b/c/flow.el.xml");
			flowExecutor.reloadRule();
			return flowExecutor.execute2Resp("chain1", "arg").isSuccess();
		});
	}

	@Test
	public void testAbsolutePathMatch() throws Exception {
		Assertions.assertTrue(() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource("C:/LiteFlow/Tes*/**/c/*.el.xml");
			flowExecutor.reloadRule();
			return flowExecutor.execute2Resp("chain1", "arg").isSuccess();
		});
	}

	@BeforeAll
	public static void createFiles() {
		String filePath = "C:/LiteFlow/Test/a/b/c";
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">WHEN(a, b, c);</chain></flow>";
		FileUtil.mkdir(filePath);
		FileUtil.writeString(content, filePath + "/flow.el.xml", CharsetUtil.CHARSET_UTF_8);
	}

	@AfterAll
	public static void removeFiles() {
		FileUtil.del("C:/LiteFlow");
	}

}
