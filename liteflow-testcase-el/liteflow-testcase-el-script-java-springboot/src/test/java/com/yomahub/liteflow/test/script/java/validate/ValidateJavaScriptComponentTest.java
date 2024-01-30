package com.yomahub.liteflow.test.script.java.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.java.JavaExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ValidateJavaScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateJavaScriptComponentTest {
    @Test
    public void testJavaScriptComponentValidateFunction(){
        String correctScript = "import com.alibaba.fastjson2.JSON;\n" +
                "            import com.yomahub.liteflow.slot.DefaultContext;\n" +
                "            import com.yomahub.liteflow.spi.holder.ContextAwareHolder;\n" +
                "            import com.yomahub.liteflow.test.script.java.common.cmp.TestDomain;\n" +
                "            import com.yomahub.liteflow.script.body.JaninoCommonScriptBody;\n" +
                "            import com.yomahub.liteflow.script.ScriptExecuteWrap;\n" +
                "\n" +
                "            public class Demo implements JaninoCommonScriptBody {\n" +
                "                public Void body(ScriptExecuteWrap wrap) {\n" +
                "                    int v1 = 2;\n" +
                "                    int v2 = 3;\n" +
                "                    DefaultContext ctx = (DefaultContext) wrap.getCmp().getFirstContextBean();\n" +
                "                    ctx.setData(\"s1\", v1 * v2);\n" +
                "\n" +
                "                    TestDomain domain = (TestDomain) ContextAwareHolder.loadContextAware().getBean(TestDomain.class);\n" +
                "                    System.out.println(JSON.toJSONString(domain));\n" +
                "                    String str = domain.sayHello(\"jack\");\n" +
                "                    ctx.setData(\"hi\", str);\n" +
                "\n" +
                "                    return null;\n" +
                "                }\n" +
                "            }";
        // 未指定类型名错误
        String wrongScript = "import com.alibaba.fastjson2.JSON;\n" +
                "            import com.yomahub.liteflow.slot.DefaultContext;\n" +
                "            import com.yomahub.liteflow.spi.holder.ContextAwareHolder;\n" +
                "            import com.yomahub.liteflow.test.script.java.common.cmp.TestDomain;\n" +
                "            import com.yomahub.liteflow.script.body.JaninoCommonScriptBody;\n" +
                "            import com.yomahub.liteflow.script.ScriptExecuteWrap;\n" +
                "\n" +
                "            public class Demo implements JaninoCommonScriptBody {\n" +
                "                public Void body(ScriptExecuteWrap wrap) {\n" +
                "                    v1 = 2;\n" +
                "                    int v2 = 3;\n" +
                "                    DefaultContext ctx = (DefaultContext) wrap.getCmp().getFirstContextBean();\n" +
                "                    ctx.setData(\"s1\", v1 * v2);\n" +
                "\n" +
                "                    TestDomain domain = (TestDomain) ContextAwareHolder.loadContextAware().getBean(TestDomain.class);\n" +
                "                    System.out.println(JSON.toJSONString(domain));\n" +
                "                    String str = domain.sayHello(\"jack\");\n" +
                "                    ctx.setData(\"hi\", str);\n" +
                "\n" +
                "                    return null;\n" +
                "                }\n" +
                "            }";
        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.JAVA));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.GROOVY));
    }
}
