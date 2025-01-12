package com.yomahub.liteflow.test.script.javax.scriptExtends;

import com.yomahub.liteflow.core.FlowExecutor;
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
@TestPropertySource(value = "classpath:/scriptExtends/application.properties")
@SpringBootTest(classes = ScriptJavaxScriptExtendsELTest.class)
@EnableAutoConfiguration
public class ScriptJavaxScriptExtendsELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    // 测试普通脚本节点
    @Test
    public void testCommon1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
    }
}
