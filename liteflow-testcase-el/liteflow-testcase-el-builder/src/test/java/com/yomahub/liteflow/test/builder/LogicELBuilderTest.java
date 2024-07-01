package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.*;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * 与或非表达式测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = LogicELBuilderTest.class)
@EnableAutoConfiguration
public class LogicELBuilderTest extends BaseTest {
    // 与或非表达式调用 测试
    @Test
    public void testlogic1(){
        String expectedStr = "AND(node(\"a\"),OR(node(\"b\"),node(\"c\")),NOT(node(\"d\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not("d")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not("d")).toEL()));
    }
    @Test
    public void testlogic2(){
        String expectedStr = "AND(\n\tnode(\"a\"),\n\tOR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tNOT(\n\t\tnode(\"d\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not("d")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not("d")).toEL(true)));
    }

    @Test
    public void testlogic3(){
        String expectedStr = "AND(node(\"a\"),OR(node(\"b\"),node(\"c\")),NOT(node(\"d\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a").and(ELBus.or("b").or("c")).and(ELBus.not("d")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a").and(ELBus.or("b").or("c")).and(ELBus.not("d")).toEL()));
    }

    @Test
    public void testlogic4(){
        String expectedStr = "AND(\n\tnode(\"a\"),\n\tOR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tNOT(\n\t\tnode(\"d\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a").and(ELBus.or("b").or("c")).and(ELBus.not("d")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a").and(ELBus.or("b").or("c")).and(ELBus.not("d")).toEL(true)));
    }
    // 属性设置
    @Test
    public void testlogic5(){
        String expectedStr = "AND(node(\"a\"),OR(node(\"b\"),node(\"c\")).id(\"this is a id\"),NOT(node(\"d\")).tag(\"this is a tag\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c").id("this is a id"), ELBus.not("d").tag("this is a tag")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c").id("this is a id"), ELBus.not("d").tag("this is a tag")).toEL()));
    }
    @Test
    public void testlogic6(){
        String expectedStr = "AND(\n\tnode(\"a\"),\n\tOR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tNOT(\n\t\tnode(\"d\")\n\t).tag(\"this is a tag\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c").id("this is a id"), ELBus.not("d").tag("this is a tag")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c").id("this is a id"), ELBus.not("d").tag("this is a tag")).toEL(true)));
    }
    @Test
    public void testlogic7(){
        String expectedStr = "andData = '{\"name\":\"zhangsan\",\"age\":18}';\nAND(node(\"a\"),OR(node(\"b\"),node(\"c\")),NOT(node(\"d\").data(andData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("andData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("andData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL()));
    }
    @Test
    public void testlogic8(){
        String expectedStr = "andData = '{\"name\":\"zhangsan\",\"age\":18}';\nAND(\n\tnode(\"a\"),\n\tOR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tNOT(\n\t\tnode(\"d\").data(andData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("andData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("andData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL(true)));
    }
    @Test
    public void testlogic9(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "orData = '{\"name\":\"zhangsan\",\"age\":18}';\nAND(node(\"a\"),OR(node(\"b\"),node(\"c\")),NOT(node(\"d\").data(orData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("orData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("orData", name2Value))).toEL()));
    }
    @Test
    public void testlogic10(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "orData = '{\"name\":\"zhangsan\",\"age\":18}';\nAND(\n\tnode(\"a\"),\n\tOR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tNOT(\n\t\tnode(\"d\").data(orData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("orData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("orData", name2Value))).toEL(true)));
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
    @Test
    public void testlogic11(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String expectedStr = "notData = '{\"name\":\"zhangsan\",\"age\":18}';\nAND(node(\"a\"),OR(node(\"b\"),node(\"c\")),NOT(node(\"d\").data(notData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("notData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("notData", name2Value))).toEL()));
    }
    @Test
    public void testlogic12(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String expectedStr = "notData = '{\"name\":\"zhangsan\",\"age\":18}';\nAND(\n\tnode(\"a\"),\n\tOR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tNOT(\n\t\tnode(\"d\").data(notData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("notData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.and("a", ELBus.or("b", "c"), ELBus.not(ELBus.node("d").data("notData", name2Value))).toEL(true)));
    }
    // NOT调用方法补充测试
    @Test
    public void testLogic13(){
        String expectedStr = "NOT(node(\"a\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.node("a")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.node("a")).toEL()));
        expectedStr = "NOT(AND(node(\"a\"),node(\"b\"),node(\"c\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.and("a", "b", "c")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.and("a", "b", "c")).toEL()));
        expectedStr = "NOT(OR(node(\"a\"),node(\"b\"),node(\"c\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.or("a", "b", "c")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.or("a", "b", "c")).toEL()));
        expectedStr = "NOT(NOT(node(\"a\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.not(ELBus.node("a"))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.not(ELBus.node("a"))).toEL()));
    }
    @Test
    public void testLogic14(){
        String expectedStr = "NOT(\n\tnode(\"a\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.node("a")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.node("a")).toEL(true)));
        expectedStr = "NOT(\n\tAND(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.and("a", "b", "c")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.and("a", "b", "c")).toEL(true)));
        expectedStr = "NOT(\n\tOR(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.or("a", "b", "c")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.or("a", "b", "c")).toEL(true)));
        expectedStr = "NOT(\n\tNOT(\n\t\tnode(\"a\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.not(ELBus.not(ELBus.node("a"))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.not(ELBus.not(ELBus.node("a"))).toEL(true)));
    }

    @Test
    public void testLogic(){
        AndELWrapper andEl = ELBus.and("a", "b").id("this is a id").tag("this is a tag");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(andEl.toEL()));
        OrELWrapper orEl = ELBus.or("a", "b");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(orEl.toEL()));
        NotELWrapper notEl = ELBus.not("a");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(notEl.toEL()));
    }
}
