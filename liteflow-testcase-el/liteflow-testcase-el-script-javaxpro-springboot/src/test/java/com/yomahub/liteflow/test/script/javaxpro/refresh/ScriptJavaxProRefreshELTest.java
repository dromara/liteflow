package com.yomahub.liteflow.test.script.javaxpro.refresh;

import cn.hutool.core.io.FileUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/refresh/application.properties")
@SpringBootTest(classes = ScriptJavaxProRefreshELTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.script.javaxpro.refresh.cmp" })
public class ScriptJavaxProRefreshELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testRefresh1() {
        String scriptContent = FileUtil.readUtf8String("refresh/s1.java");
        LiteFlowNodeBuilder.createScriptNode().setId("s1")
                .setScript(scriptContent)
                .setLanguage(ScriptTypeEnum.JAVA.getDisplayName())
                .build();
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", null, DefaultContext.class);
        DefaultContext context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("1", context.getData("testFlag"));

        //改写脚本
        scriptContent = FileUtil.readUtf8String("refresh/s1_update.java");
        FlowBus.reloadScript("s1", scriptContent);
        response = flowExecutor.execute2Resp("chain1", null, DefaultContext.class);
        context = response.getFirstContextBean();
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("2", context.getData("testFlag"));

    }

}