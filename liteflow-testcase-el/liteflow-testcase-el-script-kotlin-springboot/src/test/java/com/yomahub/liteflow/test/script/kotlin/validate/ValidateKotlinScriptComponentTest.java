package com.yomahub.liteflow.test.script.kotlin.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.ScriptExecutorFactory;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import javax.script.Compilable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@SpringBootTest(classes = ValidateKotlinScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateKotlinScriptComponentTest {
    @Test
    public void testScriptComponentValidateFunction() throws Exception {
        // 编译错误，字符串不能直接赋值给Int
        String wrongScript = "val number: Int = \"123\"";
        // 使用转换函数
        String correctScript = "val number: Int = \"123\".toInt()";

        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.KOTLIN));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.JS));
    }
}
