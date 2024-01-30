package com.yomahub.liteflow.test.script.lua.validate;

import com.yomahub.liteflow.enums.ScriptTypeEnum;
import com.yomahub.liteflow.script.lua.LuaScriptExecutor;
import com.yomahub.liteflow.script.validator.ScriptValidator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ValidateLuaScriptComponentTest.class)
@EnableAutoConfiguration
public class ValidateLuaScriptComponentTest {
    @Test
    public void testLuaScriptComponentValidateFunction(){
        String correctScript = "                local a=6\n" +
                "                local b=10\n" +
                "                if(a>5) then\n" +
                "                    b=5\n" +
                "                else\n" +
                "                    b=2\n" +
                "                end\n" +
                "                defaultContext:setData(\"s1\",a*b)\n" +
                "                defaultContext:setData(\"s2\",_meta:get(\"nodeId\"))";
        // 语法错误
        String wrongScript = "                local a=6\n" +
                "                local b=10\n" +
                "                if(a>5) tn\n" +
                "                    b=5\n" +
                "                else\n" +
                "                    b=2\n" +
                "                end\n" +
                "                defaultContext:setData(\"s1\",a*b)\n" +
                "                defaultContext:setData(\"s2\",_meta:get(\"nodeId\"))";
        Assertions.assertTrue(ScriptValidator.validate(correctScript));
        Assertions.assertFalse(ScriptValidator.validate(wrongScript));

        Assertions.assertTrue(ScriptValidator.validate(correctScript, ScriptTypeEnum.LUA));
        Assertions.assertFalse(ScriptValidator.validate(correctScript, ScriptTypeEnum.JS));
    }
}
