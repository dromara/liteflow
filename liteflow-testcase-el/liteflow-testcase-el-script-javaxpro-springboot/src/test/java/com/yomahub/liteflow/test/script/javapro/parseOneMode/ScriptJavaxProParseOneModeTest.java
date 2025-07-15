package com.yomahub.liteflow.test.script.javapro.parseOneMode;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:/parseOneMode/application.properties")
@SpringBootTest(classes = ScriptJavaxProParseOneModeTest.class)
@EnableAutoConfiguration
public class ScriptJavaxProParseOneModeTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testParseOneMode() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
    }

}