package com.yomahub.liteflow.test.monitorFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;

public class LiteflowMonitorFileTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init() {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("monitorFile/flow.el.xml");
        config.setEnableMonitorFile(true);
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    @Test
    public void testMultipleType() throws InterruptedException {
        String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
        String content = FileUtil.readUtf8String(absolutePath);
        String newContent = content.replace("THEN(a, b, c);", "THEN(a, c, b);");
        FileUtil.writeString(newContent, new File(absolutePath), CharsetUtil.CHARSET_UTF_8);

        Thread.sleep(1000);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>c==>b", response.getExecuteStepStr());
    }

}
