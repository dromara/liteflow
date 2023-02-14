package com.yomahub.liteflow.test.script.groovy.monitorFile;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.resource.ClassPathResource;
import cn.hutool.core.util.CharsetUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
import java.io.File;

@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/monitorFile/application.properties")
@SpringBootTest(classes = MonitorFileGroovyELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.groovy.monitorFile.cmp"})
public class MonitorFileGroovyELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testMonitor() throws Exception{
        String absolutePath = new ClassPathResource("classpath:/monitorFile/s1.groovy").getAbsolutePath();
        String content = FileUtil.readUtf8String(absolutePath);
        String newContent = content.replace("a=3", "a=2");
        FileUtil.writeString(newContent,new File(absolutePath), CharsetUtil.CHARSET_UTF_8);

        Thread.sleep(1500);

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(Integer.valueOf(4), context.getData("s1"));

    }
}
