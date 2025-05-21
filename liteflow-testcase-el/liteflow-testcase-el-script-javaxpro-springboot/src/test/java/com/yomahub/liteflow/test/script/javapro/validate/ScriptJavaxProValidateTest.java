package com.yomahub.liteflow.test.script.javapro.validate;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.common.entity.ValidationResp;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.script.validator.ScriptValidator;
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
@TestPropertySource(value = "classpath:/validate/application.properties")
@SpringBootTest(classes = ScriptJavaxProValidateTest.class)
@EnableAutoConfiguration
public class ScriptJavaxProValidateTest extends BaseTest {

    @Test
    public void testValidate() {
        String script = ResourceUtil.readUtf8Str("validate/s1.java");

        ValidationResp resp = ScriptValidator.validateWithEx(script);

        Assertions.assertFalse(resp.isSuccess());
    }
}