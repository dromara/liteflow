package com.yomahub.liteflow.test.script.qlexpress.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.qlexpress.QLExpressScriptExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ValidateQLExpressScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateQLExpressScriptComponentTest {
    @Test
    public void testQLExpressScriptComponentValidateFunction(){
        String correctScript = "                count = defaultContext.getData(\"count\");\n" +
                "                if(count > 100){\n" +
                "                    return \"a\";\n" +
                "                }else{\n" +
                "                    return \"b\";\n" +
                "                }";
        // 语法错误
        String wrongScript = "                count = defaultContext.getData(\"count\");\n" +
                "                if(count > 100){\n" +
                "                    return \"a\";\n" +
                "                }el{\n" +
                "                    return \"b\";\n" +
                "                }";
        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.QLEXPRESS));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.PYTHON));
    }
}
