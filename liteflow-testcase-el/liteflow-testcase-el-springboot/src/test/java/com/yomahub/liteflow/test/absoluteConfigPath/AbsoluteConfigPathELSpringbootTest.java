package com.yomahub.liteflow.test.absoluteConfigPath;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.CharsetUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.Objects;

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


	private static String rootDir;

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testAbsoluteConfig() throws Exception {
		Assertions.assertTrue(() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource(StrUtil.format("{}/sub/a/flow1.xml",rootDir));
			flowExecutor.reloadRule();
			return flowExecutor.execute2Resp("chain1", "arg").isSuccess();
		});
	}

	@Test
	public void testAbsolutePathMatch() throws Exception {
		Assertions.assertTrue(() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource(StrUtil.format("{}/sub/**/*.xml",rootDir));
			flowExecutor.reloadRule();
			return flowExecutor.execute2Resp("chain2", "arg").isSuccess();
		});
	}

	@Test
	public void testAbsPath3() throws Exception{
		Assertions.assertTrue(() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource(StrUtil.format("{}/sub/a/*.xml,sub/b/*.xml",rootDir));
			flowExecutor.reloadRule();
			return flowExecutor.execute2Resp("chain2", "arg").isSuccess();
		});
	}

	@Test
	@EnabledIf("isWindows")
	public void testAbsPath4() throws Exception{
		Assertions.assertTrue(() -> {
			LiteflowConfig config = LiteflowConfigGetter.get();
			config.setRuleSource(StrUtil.format("{}\\sub\\a\\*.xml,sub\\b\\*.xml",rootDir));
			flowExecutor.reloadRule();
			return flowExecutor.execute2Resp("chain2", "arg").isSuccess();
		});
	}

	public static boolean isWindows() {
		try {
			String osName = System.getProperty("os.name");
			if (osName.isEmpty()) return false;
			else {
				return osName.contains("Windows");
			}
		} catch (Exception e) {
			return false;
		}
	}

	@BeforeAll
	public static void createFiles() {
		rootDir = FileUtil.getAbsolutePath(ResourceUtil.getResource("").getPath());

		String path1 = StrUtil.format("{}/sub/a", rootDir);
		String path2 = StrUtil.format("{}/sub/b", rootDir);

		FileUtil.mkdir(path1);
		FileUtil.mkdir(path2);

		String content1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">WHEN(a, b, c);</chain></flow>";
		String content2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain2\">THEN(c, chain1);</chain></flow>";

		FileUtil.writeString(content1, path1 + "/flow1.xml", CharsetUtil.CHARSET_UTF_8);
		FileUtil.writeString(content2, path2 + "/flow2.xml", CharsetUtil.CHARSET_UTF_8);
	}

	@AfterAll
	public static void removeFiles() {
		FileUtil.del(StrUtil.format("{}/sub", rootDir));
	}

}
