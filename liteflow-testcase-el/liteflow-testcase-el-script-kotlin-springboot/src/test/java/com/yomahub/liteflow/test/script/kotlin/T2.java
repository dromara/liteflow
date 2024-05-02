package com.yomahub.liteflow.test.script.kotlin;

import com.yomahub.liteflow.slot.DefaultContext;
import org.junit.jupiter.api.Test;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.script.SimpleBindings;

public class T2 {
    @Test
    public  void test1() throws ScriptException {
        // 初始化 ScriptEngineManager 和 Kotlin 脚本引擎
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("kotlin");

        if (engine == null) {
            throw new IllegalStateException("Kotlin script engine not found");
        }

        // 假设我们有以下Kotlin脚本，它期望一个名为'message'的外部参数
        String script = "fun greet(name: String) {\n" +
                "                println(\"Hello, $name!\")\n" +
                "            }\n" +
                "            println(bindings[\"message\"])";


        // 编译脚本为CompiledScript对象
        Compilable compilable = (Compilable) engine;
        CompiledScript compiledScript = compilable.compile(script);

        // 准备脚本上下文，用于传递外部参数
        Bindings bindings = engine.createBindings();
        // 设置外部参数
        bindings.put("message", "User");

        // 使用相同的上下文多次执行已编译的脚本
        for (int i = 0; i < 2; i++) {
            compiledScript.eval(bindings);
        }
//        engine.put("message","User");
//        engine.eval(script);
        //engine.eval(script,bindings);
    }

    @Test
    public  void test2() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("kotlin");
        String script = "" +
                "var defaultContext = bindings[\"defaultContext\"]\n" +
                "println(defaultContext.getData(\"key\"))";
        // 编译脚本为CompiledScript对象
        Compilable compilable = (Compilable) engine;
        CompiledScript compiledScript = compilable.compile(script);

        // 准备脚本上下文，用于传递外部参数
        Bindings bindings = new SimpleBindings();
        DefaultContext context = new DefaultContext();
        context.setData("key", "value");
        // 设置外部参数
        bindings.put("defaultContext", context);
        compiledScript.eval(bindings);
    }

    @Test
    public  void test3() throws ScriptException {
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("kotlin");
        String script =
                "fun  getNum() = 15\n" +
                        "getNum()";

        // 编译脚本为CompiledScript对象
        Compilable compilable = (Compilable) engine;
        CompiledScript compiledScript = compilable.compile(script);

        // 准备脚本上下文，用于传递外部参数
//        Bindings bindings = new SimpleBindings();
//        DefaultContext context = new DefaultContext();
//        context.setData("key", "value");
//        // 设置外部参数
//        bindings.put("defaultContext", context);
  /*      Object res = compiledScript.eval();
        System.out.println(res);*/
        Object eval = engine.eval(script);
        System.out.println(eval);
    }
}
