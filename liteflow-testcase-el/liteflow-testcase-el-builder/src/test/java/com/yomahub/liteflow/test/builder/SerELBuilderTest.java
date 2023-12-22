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
@SpringBootTest(classes = SerELBuilderTest.class)
@EnableAutoConfiguration
public class SerELBuilderTest extends BaseTest {

    // then组件测试
    @Test
    public void testSer1(){
        Assertions.assertEquals("SER(node(\"a\"),node(\"b\"));",
                ELBus.ser("a", "b").toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", "b").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer2(){
        Assertions.assertEquals("SER(\n\tnode(\"a\")," +
                        "\n\tnode(\"b\")\n);",
                ELBus.ser("a", "b").toEL(true));
        System.out.println("SER(\n\tnode(\"a\")," +
                "\n\tnode(\"b\")\n);");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", "b").toEL(true)));
    }
    // then组件then方法调用测试
    @Test
    public void testSer3(){
        Assertions.assertEquals("SER(node(\"a\"),node(\"b\"),node(\"c\"));",
                ELBus.ser("a", "b").ser("c").toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", "b").ser("c").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer4(){
        Assertions.assertEquals("SER(\n\tnode(\"a\"),\n\tnode(\"b\")," +
                        "\n\tnode(\"c\")\n);",
                ELBus.ser("a", "b").ser("c").toEL(true));
        System.out.println("SER(\n\tnode(\"a\"),\n\tnode(\"b\")," +
                "\n\tnode(\"c\")\n);");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", "b").ser("c").toEL(true)));
    }
    // then组件嵌套调用测试
    @Test
    public void testSer5(){
        Assertions.assertEquals("SER(node(\"a\"),SER(node(\"b\"),node(\"c\")),node(\"d\"));",
                ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer6(){
        Assertions.assertEquals("SER(\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);",
                ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").toEL(true));
        System.out.println("SER(\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").toEL(true)));
    }
    // pre组件测试
    @Test
    public void testSer7(){
        Assertions.assertEquals("SER(PRE(node(\"p\")),PRE(node(\"pp\")),node(\"a\"),SER(node(\"b\"),node(\"c\")),node(\"d\"));",
                ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").pre("pp").toEL());
        System.out.println("SER(PRE(node(\"p\")),PRE(node(\"pp\")),node(\"a\"),SER(node(\"b\"),node(\"c\")),node(\"d\"));");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").pre("pp").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer8(){
        Assertions.assertEquals("SER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tPRE(\n\t\tnode(\"pp\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);",
                ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").pre("pp").toEL(true));
        System.out.println("SER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tPRE(\n\t\tnode(\"pp\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").pre("pp").toEL(true)));
    }
    // pre finally 格式测试
    @Test
    public void testSer9(){
        Assertions.assertEquals("SER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")),node(\"d\"),FINALLY(node(\"f\")));",
                ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").finallyOpt("f").toEL());
        System.out.println("SER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")),node(\"d\"),FINALLY(node(\"f\")));");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").finallyOpt("f").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer10(){
        Assertions.assertEquals("SER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n);",
                ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").finallyOpt("f").toEL(true));
        System.out.println("SER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n);");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c")).ser("d").pre("p").finallyOpt("f").toEL(true)));
    }
    // 属性设置测试
    @Test
    public void testSer11(){
        Assertions.assertEquals("SER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser("d").pre("p").finallyOpt("f").toEL());
        System.out.println("SER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\"),FINALLY(node(\"f\"))).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser("d").pre("p").finallyOpt("f").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer12(){
        Assertions.assertEquals("SER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser("d").pre("p").finallyOpt("f").toEL(true));
        System.out.println("SER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\"),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser("d").pre("p").finallyOpt("f").toEL(true)));
    }
    // data属性测试
    @Test
    public void testSer13(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        System.out.println(JsonUtil.toJsonString(name2Value));
        Assertions.assertEquals("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL());
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer14(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        Assertions.assertEquals("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL(true));
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL(true)));
    }
    // data属性测试 Json字符串赋值data
    @Test
    public void testSer15(){
        Assertions.assertEquals("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre("p").finallyOpt("f").toEL());
//        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre("p").finallyOpt("f").toEL()));
    }
    // 格式化输出测试 Json字符串赋值data
    @Test
    public void testSer16(){
        Assertions.assertEquals("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre("p").finallyOpt("f").toEL(true));
//        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre("p").finallyOpt("f").toEL(true)));
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
    public void testSer17(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        Assertions.assertEquals("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL());
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(PRE(node(\"p\")),node(\"a\"),SER(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\").data(thenData),FINALLY(node(\"f\"))).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer18(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        Assertions.assertEquals("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");",
                ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL(true));
        System.out.println("thenData = '{\"name\":\"zhangsan\",\"age\":18}';\nSER(\n\tPRE(\n\t\tnode(\"p\")\n\t),\n\tnode(\"a\"),\n\tSER(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\").data(thenData),\n\tFINALLY(\n\t\tnode(\"f\")\n\t)\n).tag(\"this is a tag\");");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a", ELBus.ser("b").ser("c").id("this is a id")).tag("this is a tag").ser(ELBus.node("d").data("thenData", name2Value)).pre("p").finallyOpt("f").toEL(true)));
    }
    // maxWaitSecond测试
    @Test
    public void testSer19(){
        String expectedStr = "SER(node(\"a\"),node(\"b\")).maxWaitSeconds(5);";
        Assertions.assertEquals(expectedStr,
                ELBus.ser("a").ser("b").maxWaitSeconds(5).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a").ser("b").maxWaitSeconds(5).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testSer20(){
        String expectedStr = "SER(\n\tnode(\"a\"),\n\tnode(\"b\")\n).maxWaitSeconds(5);";
        Assertions.assertEquals(expectedStr,
                ELBus.ser("a").ser("b").maxWaitSeconds(5).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.ser("a").ser("b").maxWaitSeconds(5).toEL(true)));
    }
}
