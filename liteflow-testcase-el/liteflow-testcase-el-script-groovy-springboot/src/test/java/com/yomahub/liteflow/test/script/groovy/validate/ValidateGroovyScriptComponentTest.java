package com.yomahub.liteflow.test.script.groovy.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.groovy.GroovyScriptExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ValidateGroovyScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateGroovyScriptComponentTest {
    @Test
    public void testGroovyScriptComponentValidateFunction(){
        String correctScript = "            import cn.hutool.core.collection.ListUtil\n" +
                "            import cn.hutool.core.date.DateUtil\n" +
                "\n" +
                "            import java.util.function.Consumer\n" +
                "            import java.util.function.Function\n" +
                "            import java.util.stream.Collectors\n" +
                "\n" +
                "            def date = DateUtil.parse(\"2022-10-17 13:31:43\")\n" +
                "            println(date)\n" +
                "            defaultContext.setData(\"demoDate\", date)\n" +
                "\n" +
                "            List<String> list = ListUtil.toList(\"a\", \"b\", \"c\")\n" +
                "\n" +
                "            List<String> resultList = list.stream().map(s -> \"hello,\" + s).collect(Collectors.toList())\n" +
                "\n" +
                "            defaultContext.setData(\"resultList\", resultList)\n" +
                "\n" +
                "            class Student {\n" +
                "                int studentID\n" +
                "                String studentName\n" +
                "            }\n" +
                "\n" +
                "            Student student = new Student()\n" +
                "            student.studentID = 100301\n" +
                "            student.studentName = \"张三\"\n" +
                "            defaultContext.setData(\"student\", student)\n" +
                "\n" +
                "            def a = 3\n" +
                "            def b = 2\n" +
                "            defaultContext.setData(\"s1\", a * b)";
        // 语法错误
        String wrongScript = "            import cn.hutool.core.collection.ListUtil\n" +
                "            import cn.hutool.core.date.DateUtil\n" +
                "\n" +
                "            import java.util.function.Consumer\n" +
                "            import java.util.function.Function\n" +
                "            import java.util.stream.Collectors\n" +
                "\n" +
                "            d date = DateUtil.parse(\"2022-10-17 13:31:43\")\n" +
                "            println(date)\n" +
                "            defaultContext.setData(\"demoDate\", date)\n" +
                "\n" +
                "            List<String> list = ListUtil.toList(\"a\", \"b\", \"c\")\n" +
                "\n" +
                "            List<String> resultList = list.stream().map(s -> \"hello,\" + s).collect(Collectors.toList())\n" +
                "\n" +
                "            defaultContext.setData(\"resultList\", resultList)\n" +
                "\n" +
                "            class Student {\n" +
                "                int studentID\n" +
                "                String studentName\n" +
                "            }\n" +
                "\n" +
                "            Student student = new Student()\n" +
                "            student.studentID = 100301\n" +
                "            student.studentName = \"张三\"\n" +
                "            defaultContext.setData(\"student\", student)\n" +
                "\n" +
                "            def a = 3\n" +
                "            def b = 2\n" +
                "            defaultContext.setData(\"s1\", a * b)";
        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.GROOVY));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.JS));
    }
}
