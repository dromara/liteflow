package com.yomahub.liteflow.test.monitorFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.io.File;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/monitorFile/application.properties")
@SpringBootTest(classes = MonitorFileELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.monitorFile.cmp" })
public class MonitorFileELDeclSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testMonitor() throws Exception {
		String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
		FileUtil.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>", new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
		String content = FileUtil.readUtf8String(absolutePath);
		String newContent = content.replace("THEN(a, b, c);", "THEN(a, c, b);");
		FileUtil.writeString(newContent, new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
		Thread.sleep(1000);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertEquals("a==>c==>b", response.getExecuteStepStr());
	}

	@AfterEach
	public void afterEach(){
		String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
		FileUtil.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>", new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
	}

}
