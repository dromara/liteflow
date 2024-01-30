package com.yomahub.liteflow.test.script.python.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.python.PythonScriptExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ValidatePythonScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidatePythonScriptComponentTest {
    @Test
    public void testPythonScriptComponentValidateFunction(){
        String correctScript = "                import json\n" +
                "\n" +
                "                x='{\"name\": \"杰克\", \"age\": 75, \"nationality\": \"China\"}'\n" +
                "                jsonData=json.loads(x)\n" +
                "                name=jsonData['name']\n" +
                "                defaultContext.setData(\"name\", name.decode('utf-8'))\n" +
                "\n" +
                "\n" +
                "                a=6\n" +
                "                b=10\n" +
                "                if a>5:\n" +
                "                    b=5\n" +
                "                    print '你好'.decode('UTF-8')\n" +
                "                else:\n" +
                "                    print 'hi'\n" +
                "                defaultContext.setData(\"s1\",a*b)\n" +
                "                defaultContext.setData(\"td\", td.sayHi(\"jack\"))";
        // 语法错误 缩进
        String wrongScript = "                import json\n" +
                "\n" +
                "                x='{\"name\": \"杰克\", \"age\": 75, \"nationality\": \"China\"}'\n" +
                "                jsonData=json.loads(x)\n" +
                "                name=jsonData['name']\n" +
                "                defaultContext.setData(\"name\", name.decode('utf-8'))\n" +
                "\n" +
                "\n" +
                "                a=6\n" +
                "                b=10\n" +
                "                if a>5:\n" +
                "                b=5\n" +
                "                    print '你好'.decode('UTF-8')\n" +
                "                else:\n" +
                "                    print 'hi'\n" +
                "                defaultContext.setData(\"s1\",a*b)\n" +
                "                defaultContext.setData(\"td\", td.sayHi(\"jack\"))";
        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.PYTHON));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.LUA));
    }
}
