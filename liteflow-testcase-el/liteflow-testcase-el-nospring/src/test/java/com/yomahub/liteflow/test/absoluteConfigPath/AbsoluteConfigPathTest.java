package com.yomahub.liteflow.test.absoluteConfigPath;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * 非spring环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.11
 */
public class AbsoluteConfigPathTest extends BaseTest {

	private static FlowExecutor flowExecutor;


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
		String content = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
				"<flow>\n" +
				"<nodes>\n" +
				"    <node id=\"a\" class=\"com.yomahub.liteflow.test.absoluteConfigPath.cmp.ACmp\"/>\n" +
				"    <node id=\"b\" class=\"com.yomahub.liteflow.test.absoluteConfigPath.cmp.BCmp\"/>\n" +
				"    <node id=\"c\" class=\"com.yomahub.liteflow.test.absoluteConfigPath.cmp.CCmp\"/>\n" +
				"</nodes>\n" +
				"\n" +
				"<chain name=\"chain1\">\n" +
				"    WHEN(a,b,c);\n" +
				"</chain>\n" +
				"</flow>";
		FileUtil.mkdir(filePath);
		FileUtil.writeString(content, filePath + "/flow.el.xml", CharsetUtil.CHARSET_UTF_8);
		LiteflowConfig config = new LiteflowConfig();
		config.setRuleSource("absoluteConfigPath/flow.el.xml");
		flowExecutor = FlowExecutorHolder.loadInstance(config);
	}

	@AfterAll
	public static void removeFiles() {
		FileUtil.del("C:/LiteFlow");
	}
}
