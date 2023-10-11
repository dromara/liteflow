package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * 串行组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = ThenELBuilderTest.class)
@EnableAutoConfiguration
public class ThenELBuilderTest extends BaseTest {

    // then组件测试
    @Test
    public void testThen1(){
        Assertions.assertEquals(ELBus.then("a", "b").toEL(), "THEN(node(\"a\"),node(\"b\"))");
    }
    // 格式化输出测试
    @Test
    public void testThen2(){
        Assertions.assertEquals(ELBus.then("a", "b").toEL(true),
                "THEN(\n\tnode(\"a\")," +
                        "\n\tnode(\"b\")\n)");
        System.out.println("THEN(\n\tnode(\"a\")," +
                "\n\tnode(\"b\")\n)");
    }
    // then组件then方法调用测试
    @Test
    public void testThen3(){
        Assertions.assertEquals(ELBus.then("a", "b").then("c").toEL(),
                "THEN(node(\"a\"),node(\"b\"),node(\"c\"))");
    }
    // 格式化输出测试
    @Test
    public void testThen4(){
        Assertions.assertEquals(ELBus.then("a", "b").then("c").toEL(true),
                "THEN(\n\tnode(\"a\"),\n\tnode(\"b\")," +
                        "\n\tnode(\"c\")\n)");
        System.out.println("THEN(\n\tnode(\"a\"),\n\tnode(\"b\")," +
                "\n\tnode(\"c\")\n)");
    }
    // then组件嵌套调用测试
    @Test
    public void testThen5(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c")).then("d").toEL(),
                "THEN(node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"))");
    }
    // 格式化输出测试
    @Test
    public void testThen6(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c")).then("d").toEL(true),
                "THEN(\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n)");
        System.out.println("THEN(\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n)");
    }
    // pre组件测试
    @Test
    public void testThen7(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c")).then("d").pre("p").pre("pp").toEL(),
                "THEN(PRE(node(\"p\")),PRE(node(\"pp\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"))");
        System.out.println("THEN(PRE(node(\"p\")),PRE(node(\"pp\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"))");
    }
    // 格式化输出测试
    @Test
    public void testThen8(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c")).then("d").pre("p").pre("pp").toEL(true),
                "THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tPRE(\n\t\tnode(\"pp\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n)");
        System.out.println("THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tPRE(\n\t\tnode(\"pp\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n)");
    }
    // pre finally 格式测试
    @Test
    public void testThen9(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c")).then("d").pre("p").finallyOpt("f").toEL(),
                "THEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"),FINALLY(node(\"f\")))");
        System.out.println("THEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"),FINALLY(node(\"f\")))");
    }
    // 格式化输出测试
    @Test
    public void testThen10(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c")).then("d").pre("p").finallyOpt("f").toEL(true),
                "THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n)");
        System.out.println("THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n)");
    }
    // 属性设置测试
    @Test
    public void testThen11(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").pre("p").finallyOpt("f").toEL(),
                "THEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\")");
        System.out.println("THEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\")");
    }
    // 格式化输出测试
    @Test
    public void testThen12(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").pre("p").finallyOpt("f").toEL(true),
                "THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\")");
        System.out.println("THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\")");
    }
    // data属性测试
    @Test
    public void testThen13(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        System.out.println(JsonUtil.toJsonString(name2Value));
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").data("thenData", name2Value).pre("p").finallyOpt("f").toEL(),
                "thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\").data(thenData)");
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\").data(thenData)");
    }
    // 格式化输出测试
    @Test
    public void testThen14(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").data("thenData", name2Value).pre("p").finallyOpt("f").toEL(true),
                "thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\").data(thenData)");
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\").data(thenData)");
    }
    // data属性测试 Json字符串赋值data
    @Test
    public void testThen15(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}").pre("p").finallyOpt("f").toEL(),
                "thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\").data(thenData)");
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\").data(thenData)");
    }
    // 格式化输出测试 Json字符串赋值data
    @Test
    public void testThen16(){
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}").pre("p").finallyOpt("f").toEL(true),
                "thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\").data(thenData)");
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\").data(thenData)");
    }
    private static class ParamClass{
        private String name;
        private Integer age;
        public String getName(){
            return name;
        }
        public Integer getAge(){
            return age;
        }
    }
    // data属性测试
    @Test
    public void testThen17(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").data("thenData", name2Value).pre("p").finallyOpt("f").toEL(),
                "thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\").data(thenData)");
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\").data(thenData)");
    }
    // 格式化输出测试
    @Test
    public void testThen18(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        Assertions.assertEquals(ELBus.then("a", ELBus.then("b").then("c").id("this is a id")).tag("this is a tag").then("d").data("thenData", name2Value).pre("p").finallyOpt("f").toEL(true),
                "thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\").data(thenData)");
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\").data(thenData)");
    }
    // maxWaitSecond测试
    @Test
    public void testThen19(){
        String actualStr = "THEN(node(\"a\"),node(\"b\")).maxWaitSeconds(5)";
        Assertions.assertEquals(ELBus.then("a").then("b").maxWaitSeconds(5).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testThen20(){
        String actualStr = "THEN(\n\tnode(\"a\"),\n\tnode(\"b\")\n).maxWaitSeconds(5)";
        Assertions.assertEquals(ELBus.then("a").then("b").maxWaitSeconds(5).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
}
