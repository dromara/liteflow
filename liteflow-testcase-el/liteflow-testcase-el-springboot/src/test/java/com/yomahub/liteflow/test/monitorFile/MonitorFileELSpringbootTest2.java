package com.yomahub.liteflow.test.monitorFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

@TestPropertySource(value = "classpath:/monitorFile/application2.properties")
@SpringBootTest(classes = MonitorFileELSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.monitorFile.cmp" })
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MonitorFileELSpringbootTest2 extends BaseTest {

	@Resource
	private FlowExecutor flowExecutor;

    // 测试监听新增文件
    @Test
    public void testMonitorAddFile() throws Exception {
        // 文件新增前
        LiteflowResponse liteflowResponse = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertFalse(liteflowResponse.isSuccess());

        // 文件新增
        String flowPath = new ClassPathResource("classpath:monitorFile").getAbsolutePath();
        Path newFilePath = Paths.get(flowPath, "test", "flow.el.2.xml");
        FileUtil.writeString("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
                        "<flow><chain id=\"chain2\">THEN(a);</chain></flow>",
                newFilePath.toFile(), CharsetUtil.CHARSET_UTF_8);
        Thread.sleep(1000);
        liteflowResponse = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(liteflowResponse.isSuccess());
        Assertions.assertEquals("a", liteflowResponse.getExecuteStepStr());
    }

    @AfterEach
    public void afterEach(){
        // 删除新增文件
        String flowPath = new ClassPathResource("classpath:monitorFile").getAbsolutePath();
        Path newFilePath = Paths.get(flowPath, "test", "flow.el.2.xml");
        FileUtil.del(newFilePath.getParent());
    }

}
