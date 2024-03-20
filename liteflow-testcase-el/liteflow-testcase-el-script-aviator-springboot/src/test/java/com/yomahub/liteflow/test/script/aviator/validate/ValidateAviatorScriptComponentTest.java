package com.yomahub.liteflow.test.script.aviator.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.aviator.AviatorScriptExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@SpringBootTest(classes = ValidateAviatorScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateAviatorScriptComponentTest {

    @Test
    public void testAviatorScriptComponentValidateFunction(){
        String correctScript = "                use java.util.Date;\n" +
                "                use cn.hutool.core.date.DateUtil;\n" +
                "                let d = DateUtil.formatDateTime(new Date());\n" +
                "                println(d);\n" +
                "\n" +
                "                a = 2;\n" +
                "                b = 3;\n" +
                "\n" +
                "                setData(defaultContext, \"s1\", a*b);";
        // 语法错误
        String wrongScript = "                use java.util.Date;\n" +
                "                use cn.hutool.core.date.DateUtil;\n" +
                "                lt d = DateUtil.formatDateTime(new Date());\n" +
                "                println(d);\n" +
                "\n" +
                "                a = 2;\n" +
                "                b = 3;\n" +
                "\n" +
                "                setData(defaultContext, \"s1\", a*b);";
        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.AVIATOR));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.PYTHON));


    }
}
