package com.yomahub.liteflow.test.script.graalvm.common;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.graalvm.polyglot.Context;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


/**
 * 测试springboot下的groovy脚本组件，基于xml配置
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/common/application.properties")
@SpringBootTest(classes = LiteflowXmlScriptJsCommonELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.graalvm.common.cmp"})
public class LiteflowXmlScriptJsCommonELTest extends BaseTest {

//    @Resource
//    private FlowExecutor flowExecutor;
//
//    //测试普通脚本节点
//    @Test
//    public void testCommon1() {
//        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
//        DefaultContext context = response.getFirstContextBean();
//        Assert.assertTrue(response.isSuccess());
//        Assert.assertEquals(Double.valueOf(11), context.getData("s1"));
//    }

    /**
     *   polyglot 模式
     */
    @Test
    public  void method1()  {
        System.out.println("Hello Java!");
        try (Context context = Context.newBuilder().allowAllAccess(true).build()) {
            context.eval("js", "print('Hello JavaScript!');");
            context.eval("js", "let user = {name:\"dalong\",age:333}; print(JSON.stringify(user))");
            java.math.BigDecimal v = context.eval("js",
                    "var BigDecimal = Java.type('java.math.BigDecimal');" +
                            "BigDecimal.valueOf(10).pow(20)")
                    .asHostObject();
            System.out.println(v.toString());
        }
    }

    /**
     * 一种是基于ScriptEngineManager模式
     * @throws ScriptException
     * @throws NoSuchMethodException
     */
    @Test
    public void method2() throws ScriptException, NoSuchMethodException {
        //   注意此处可以直接使用js，因为js-scriptengine 的spi 注册的时候会自动处理了内置的nashorn
        ScriptEngine eng = new ScriptEngineManager().getEngineByName("js");
        eng.eval("let user = {name:\"dalong\",age:333}; print(JSON.stringify(user))");
    }
}
