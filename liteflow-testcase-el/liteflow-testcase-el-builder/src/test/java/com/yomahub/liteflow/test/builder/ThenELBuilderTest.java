package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
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

    @Test
    public void testThen0(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expected = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(\n\ta.tag(\"tagA\").data(nodeData).maxWaitSeconds(10).retry(2),\n\tnode(\"b\")\n);";
        String actualEl = ELBus.then(ELBus.element("a").data("nodeData", name2Value).tag("tagA").maxWaitSeconds(10).retry(2), ELBus.node("b")).toEL(true);
        Assertions.assertEquals(expected, actualEl);
    }

    // then组件测试
    @Test
    public void testThen1(){
        Assertions.assertEquals("THEN(node(\"a\"),node(\"b\"));",
                ELBus.then(ELBus.node("a"), ELBus.node("b")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.node("b")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen2(){
        Assertions.assertEquals("THEN(\n\tnode(\"a\")," +
                        "\n\tnode(\"b\")\n);",
                ELBus.then(ELBus.node("a"), ELBus.node("b")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.node("b")).toEL(true)));
    }
    // then组件then方法调用测试
    @Test
    public void testThen3(){
        Assertions.assertEquals("THEN(node(\"a\"),node(\"b\"),node(\"c\"));",
                ELBus.then(ELBus.node("a"), ELBus.node("b")).then(ELBus.node("c")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.node("b")).then(ELBus.node("c")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen4(){
        Assertions.assertEquals("THEN(\n\tnode(\"a\"),\n\tnode(\"b\")," +
                        "\n\tnode(\"c\")\n);",
                ELBus.then(ELBus.node("a"), ELBus.node("b")).then(ELBus.node("c")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.node("b")).then(ELBus.node("c")).toEL(true)));
    }
    // then组件嵌套调用测试
    @Test
    public void testThen5(){
        Assertions.assertEquals("THEN(node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"));",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen6(){
        Assertions.assertEquals("THEN(\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).toEL(true)));
    }
    // pre组件测试
    @Test
    public void testThen7(){
        Assertions.assertEquals("THEN(PRE(node(\"p\")),PRE(node(\"pp\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"));",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).pre(ELBus.node("pp")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).pre(ELBus.node("pp")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen8(){
        Assertions.assertEquals("THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tPRE(\n\t\tnode(\"pp\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).pre(ELBus.node("pp")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).pre(ELBus.node("pp")).toEL(true)));
    }
    // pre finally 格式测试
    @Test
    public void testThen9(){
        Assertions.assertEquals("THEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")),node(\"d\"),FINALLY(node(\"f\")));",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen10(){
        Assertions.assertEquals("THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n);",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c"))).then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true)));
    }
    // 属性设置测试
    @Test
    public void testThen11(){
        Assertions.assertEquals("THEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen12(){
        Assertions.assertEquals("THEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true)));
    }
    // data属性测试
    @Test
    public void testThen13(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        System.out.println(JsonUtil.toJsonString(name2Value));
        Assertions.assertEquals("thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen14(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        Assertions.assertEquals("thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true)));
    }
    // data属性测试 Json字符串赋值data
    @Test
    public void testThen15(){
        Assertions.assertEquals("thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL()));
    }
    // 格式化输出测试 Json字符串赋值data
    @Test
    public void testThen16(){
        Assertions.assertEquals("thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true)));
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
        Assertions.assertEquals("thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL());
        System.out.println("thenData = \"{\"name\":\"zhangsan\",\"age\":18}\";\nTHEN(PRE(node(\"p\")),node(\"a\"),THEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen18(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        Assertions.assertEquals("thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true));
        System.out.println("thenData = \"{\"name\":\"zhangsan\",\"age\":18}\";\nTHEN(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a"), ELBus.then(ELBus.node("b")).then(ELBus.node("c")).id("this is a id")).tag("this is a tag").then(ELBus.node("d").data("thenData", name2Value)).pre(ELBus.node("p")).finallyOpt(ELBus.node("f")).toEL(true)));
    }
    // maxWaitSecond测试
    @Test
    public void testThen19(){
        String expectedStr = "THEN(node(\"a\"),node(\"b\")).maxWaitSeconds(5);";
        Assertions.assertEquals(expectedStr,
                ELBus.then(ELBus.node("a")).then(ELBus.node("b")).maxWaitSeconds(5).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a")).then(ELBus.node("b")).maxWaitSeconds(5).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testThen20(){
        String expectedStr = "THEN(\n\tnode(\"a\"),\n\tnode(\"b\")\n).maxWaitSeconds(5);";
        Assertions.assertEquals(expectedStr,
                ELBus.then(ELBus.node("a")).then(ELBus.node("b")).maxWaitSeconds(5).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.then(ELBus.node("a")).then(ELBus.node("b")).maxWaitSeconds(5).toEL(true)));
    }
}
