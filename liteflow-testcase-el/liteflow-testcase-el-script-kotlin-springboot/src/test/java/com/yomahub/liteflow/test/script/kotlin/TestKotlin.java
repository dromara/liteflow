package com.yomahub.liteflow.test.script.kotlin;

import com.yomahub.liteflow.enums.ScriptTypeEnum;

import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class TestKotlin {
    public static void main(String[] args) throws ScriptException, NoSuchMethodException {
        // 获取脚本引擎管理器
        ScriptEngineManager manager = new ScriptEngineManager();

        // 获取Kotlin脚本引擎
        ScriptEngine engine = manager.getEngineByName(ScriptTypeEnum.KOTLIN.getEngineName()); // ".kts" 是Kotlin脚本文件的扩展名

        // 检查是否找到了Kotlin脚本引擎
        if (engine == null) {
            System.out.println("No Kotlin script engine found.");
            return;
        }

        System.out.println(engine instanceof Compilable);

        // 定义一个Kotlin脚本
        String script = "println(\"Hello, Kotlin JSR 223!\")";

        Compilable compilable = (Compilable) engine;
        CompiledScript compile = compilable.compile(script);
        compile.eval();


        // 编译并执行脚本
        engine.eval(script);

        // 如果ScriptEngine也实现了Invocable接口，我们可以调用脚本中的函数
//        if (engine instanceof Invocable) {
//            Invocable inv = (Invocable) engine;
//
//            // 调用脚本中的greet函数
//            String greeting = (String) inv.invokeFunction("greet", "World");
//            System.out.println(greeting); // 输出: Hello, World!
//        } else {
//            System.out.println("The script engine does not support Invocable interface.");
//        }
    }
}
