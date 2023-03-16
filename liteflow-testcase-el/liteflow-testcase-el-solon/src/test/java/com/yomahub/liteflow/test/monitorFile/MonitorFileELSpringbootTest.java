package com.yomahub.liteflow.test.monitorFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

import java.io.File;

@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/monitorFile/application.properties")
public class MonitorFileELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testMonitor() throws Exception{
        String absolutePath = new ClassPathResource("classpath:/monitorFile/flow.el.xml").getAbsolutePath();
        String content = FileUtil.readUtf8String(absolutePath);
        String newContent = content.replace("THEN(a, b, c);", "THEN(a, c, b);");
        FileUtil.writeString(newContent,new File(absolutePath), CharsetUtil.CHARSET_UTF_8);
        Thread.sleep(2500);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>c==>b", response.getExecuteStepStr());
    }
}
