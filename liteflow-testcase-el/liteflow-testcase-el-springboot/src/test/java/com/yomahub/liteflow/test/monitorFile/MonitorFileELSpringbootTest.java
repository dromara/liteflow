package com.yomahub.liteflow.test.monitorFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.*;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.io.File;

@TestPropertySource(value = "classpath:/monitorFile/application.properties")
@SpringBootTest(classes = MonitorFileELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.monitorFile.cmp" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonitorFileELSpringbootTest extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

	@Test
	public void testMonitor() throws Exception {
		String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
        FileUtil.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>", new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
		String content = FileUtil.readUtf8String(absolutePath);
		String newContent = content.replace("THEN(a, b, c);", "THEN(a, c, b);");
        Thread.sleep(1000);
		FileUtil.writeString(newContent, new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
		Thread.sleep(3000);
		LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
		Assertions.assertEquals("a==>c==>b", response.getExecuteStepStr());
	}

    /**
     * 对绝对路径模糊匹配功能的测试
     */
    @Test
    public void testMonitorAbsolutePath() throws Exception {
        String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
        FileUtil.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>", new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
        String content = FileUtil.readUtf8String(absolutePath);
        String newContent = content.replace("THEN(a, b, c);", "THEN(a, c, b);");
        Thread.sleep(1000);
        FileUtil.writeString(newContent, new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
        Thread.sleep(3000);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertEquals("a==>c==>b", response.getExecuteStepStr());
    }

    /**
     * 测试文件变更，但是 EL 规则错误情况
     * 输出 ERROR 日志异常信息，但是不会停止监听线程，当下一次变更正确后替换为新规则
     */
    @Test
    public void testMonitorError() throws Exception {
        String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
        String content = FileUtil.readUtf8String(absolutePath);

        // 错误规则配置
        String newContent = content.replace("THEN(a, b, c);", "THEN(c, b, ;");
        FileUtil.writeString(newContent, new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
        Thread.sleep(3000);
        LiteflowResponse reloadFailedResponse = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertEquals("a==>b==>c", reloadFailedResponse.getExecuteStepStr());

        // 再次变更正确
        newContent = content.replace("THEN(a, b, c);", "THEN(c, b, a);");
        FileUtil.writeString(newContent, new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
        Thread.sleep(3000);
        LiteflowResponse reloadSuccessResponse = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertEquals("c==>b==>a", reloadSuccessResponse.getExecuteStepStr());
    }

    @AfterEach
    public void afterEach(){
        String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
        FileUtil.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow><chain name=\"chain1\">THEN(a, b, c);</chain></flow>", new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
    }

}
