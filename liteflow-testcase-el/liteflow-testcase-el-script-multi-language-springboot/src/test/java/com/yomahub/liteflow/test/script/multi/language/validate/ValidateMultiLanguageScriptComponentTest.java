package com.yomahub.liteflow.test.script.multi.language.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.ScriptExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

@SpringBootTest(classes = ValidateMultiLanguageScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateMultiLanguageScriptComponentTest {
    @Test
    public void testMultiLanguageScriptComponentValidateFunction(){
        String correctGroovyScript = "                class Student {\n" +
                "                    int studentID;\n" +
                "                    String studentName;\n" +
                "\n" +
                "                    public void setStudentID(int id){\n" +
                "                        this.studentID = id;\n" +
                "                    }\n" +
                "                }\n" +
                "\n" +
                "                Student student = new Student()\n" +
                "                student.studentID = 100301\n" +
                "                student.studentName = \"张三\"\n" +
                "                defaultContext.setData(\"student\", student)\n" +
                "\n" +
                "                def a = 3\n" +
                "                def b = 2\n" +
                "                defaultContext.setData(\"s1\", a * b)";
        String correctJavascriptScript = "                var student = defaultContext.getData(\"student\");\n" +
                "                student.setStudentID(10032);";
        String correctPythonScript = "                a = 3\n" +
                "                s1 = defaultContext.getData(\"s1\")\n" +
                "                defaultContext.setData(\"s1\",s1*a)";
        // 语法错误 缩进
        String wrongPythonScript = "                a = 3\n" +
                "                   s1 = defaultContext.getData(\"s1\")\n" +
                "                defaultContext.setData(\"s1\",s1*a)";
        // 在加载多脚本时使用默认验证方法会错误
        Assertions.assertFalse(ScriptValidator.validate(correctGroovyScript));

        // 多语言脚本验证 正确样例
        Map<ScriptTypeEnum, String> correctData = new HashMap<>();
        correctData.put(ScriptTypeEnum.GROOVY, correctGroovyScript);
        correctData.put(ScriptTypeEnum.JS, correctJavascriptScript);
        correctData.put(ScriptTypeEnum.PYTHON, correctPythonScript);
        Assertions.assertTrue(ScriptValidator.validate(correctData));

        // 多语言脚本验证 错误样例
        Map<ScriptTypeEnum, String> wrongData = new HashMap<>();
        wrongData.put(ScriptTypeEnum.GROOVY, correctGroovyScript);
        wrongData.put(ScriptTypeEnum.JS, correctJavascriptScript);
        wrongData.put(ScriptTypeEnum.PYTHON, wrongPythonScript);
        Assertions.assertFalse(ScriptValidator.validate(wrongData));
    }
}
