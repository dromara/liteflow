package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * 条件组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = IfELBuilderTest.class)
@EnableAutoConfiguration
public class IfELBuilderTest extends BaseTest {
    // if三元函数测试
    @Test
    public void testIf1(){
        String actualStr = "IF(node(\"a\"),THEN(node(\"c\"),node(\"d\")),WHEN(node(\"e\"),node(\"f\")))";
        Assertions.assertEquals(ELBus.ifOpt("a", ELBus.then("c", "d"), ELBus.when("e", "f")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf2(){
        String actualStr = "IF(\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t),\n\tWHEN(\n\t\tnode(\"e\"),\n\t\tnode(\"f\")\n\t)\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.node("a"), ELBus.then("c", "d"), ELBus.when("e", "f")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // If二元函数测试
    @Test
    public void testIf3(){
        String actualStr = "IF(node(\"a\"),THEN(node(\"b\"),node(\"c\"))).ELSE(WHEN(node(\"c\"),node(\"d\")))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.node("a"), ELBus.then("b", "c")).elseOpt(ELBus.when("c", "d")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf4(){
        String actualStr = "IF(\n\tnode(\"a\"),\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).ELSE(\n\tWHEN(\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t)\n)";
        Assertions.assertEquals(ELBus.ifOpt("a", ELBus.then("b", "c")).elseOpt(ELBus.when("c", "d")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // ELIF调用测试
    @Test
    public void testIf5(){
        String actualStr = "IF(node(\"a\"),node(\"b\")).ELIF(node(\"f1\"),node(\"c\")).ELIF(node(\"f2\"),node(\"d\")).ELSE(node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt("a", "b").elIfOpt("f1", "c").elIfOpt("f2","d").elseOpt("e").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf6(){
        String actualStr = "IF(\n\tnode(\"a\"),\n\tnode(\"b\")\n).ELIF(\n\tnode(\"f1\"),\n\tnode(\"c\")\n).ELIF(\n\tnode(\"f2\"),\n\tnode(\"d\")\n).ELSE(\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b").elIfOpt("f1", "c").elIfOpt("f2","d").elseOpt("e").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // IF嵌套调用测试
    @Test
    public void testIf7(){
        String actualStr = "IF(node(\"a\"),node(\"b\"),IF(node(\"c\"),node(\"d\")).ELSE(node(\"e\")))";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", ELBus.ifOpt("c", "d").elseOpt("e")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf8(){
        String actualStr = "IF(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tIF(\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t).ELSE(\n\t\tnode(\"e\")\n\t)\n)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", ELBus.ifOpt("c", "d").elseOpt("e")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // IF嵌套调用测试
    @Test
    public void testIf9(){
        String actualStr = "IF(node(\"a\"),node(\"b\")).ELSE(IF(node(\"c\"),node(\"d\"),node(\"e\")))";
        Assertions.assertEquals(ELBus.ifOpt("a", "b").elseOpt(ELBus.ifOpt("c", "d", "e")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf10(){
        String actualStr = "IF(\n\tnode(\"a\"),\n\tnode(\"b\")\n).ELSE(\n\tIF(\n\t\tnode(\"c\"),\n\t\tnode(\"d\"),\n\t\tnode(\"e\")\n\t)\n)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b").elseOpt(ELBus.ifOpt("c", "d", "e")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // 与表达式输出测试
    @Test
    public void testIf11(){
        String actualStr = "IF(AND(node(\"a\"),node(\"b\"),node(\"c\")),node(\"d\"),node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.and("a", "b", "c"), "d", "e").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(AND(node(\"a\"),node(\"b\"),node(\"c\")),node(\"d\")).ELSE(node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.and("a", "b", "c"), "d").elseOpt("e").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(AND(node(\"a\"),node(\"b\"),node(\"c\")),node(\"d\")).ELIF(AND(node(\"f1\"),node(\"f2\")),node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.and("a", "b", "c"), "d").elIfOpt(ELBus.and("f1", "f2"), "e").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf12(){
        String actualStr = "IF(\n\tAND(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.and("a", "b", "c"), "d", "e").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(\n\tAND(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n).ELSE(\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.and("a", "b", "c"), "d").elseOpt("e").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(\n\tAND(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n).ELIF(\n\tAND(\n\t\tnode(\"f1\"),\n\t\tnode(\"f2\")\n\t),\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.and("a", "b", "c"), "d").elIfOpt(ELBus.and("f1", "f2"), "e").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // 或表达式测试
    @Test
    public void testIf13(){
        String actualStr = "IF(OR(node(\"a\"),node(\"b\"),node(\"c\")),node(\"d\"),node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.or("a", "b", "c"), "d", "e").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(OR(node(\"a\"),node(\"b\"),node(\"c\")),node(\"d\")).ELSE(node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.or("a", "b", "c"), "d").elseOpt("e").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(OR(node(\"a\"),node(\"b\"),node(\"c\")),node(\"d\")).ELIF(OR(node(\"f1\"),node(\"f2\")),node(\"e\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.or("a", "b", "c"), "d").elIfOpt(ELBus.or("f1", "f2"), "e").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf14(){
        String actualStr = "IF(\n\tOR(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\"),\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.or("a", "b", "c"), "d", "e").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(\n\tOR(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n).ELSE(\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.or("a", "b", "c"), "d").elseOpt("e").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(\n\tOR(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n).ELIF(\n\tOR(\n\t\tnode(\"f1\"),\n\t\tnode(\"f2\")\n\t),\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.or("a", "b", "c"), "d").elIfOpt(ELBus.or("f1", "f2"), "e").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // 非表达式测试
    @Test
    public void testIf15(){
        String actualStr = "IF(NOT(node(\"a\")),node(\"b\"),node(\"c\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.not("a"), "b", "c").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(NOT(node(\"a\")),node(\"b\")).ELSE(node(\"c\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.not("a"), "b").elseOpt("c").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(NOT(node(\"a\")),node(\"b\")).ELIF(NOT(node(\"f\")),node(\"c\"))";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.not("a"), "b").elIfOpt(ELBus.not("f"), "c").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testIf16(){
        String actualStr = "IF(\n\tNOT(\n\t\tnode(\"a\")\n\t),\n\tnode(\"b\"),\n\tnode(\"c\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.not("a"), "b", "c").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(\n\tNOT(\n\t\tnode(\"a\")\n\t),\n\tnode(\"b\")\n).ELSE(\n\tnode(\"c\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.not("a"), "b").elseOpt("c").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "IF(\n\tNOT(\n\t\tnode(\"a\")\n\t),\n\tnode(\"b\")\n).ELIF(\n\tNOT(\n\t\tnode(\"f\")\n\t),\n\tnode(\"c\")\n)";
        Assertions.assertEquals(ELBus.ifOpt(ELBus.not("a"), "b").elIfOpt(ELBus.not("f"), "c").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // 属性测试
    @Test
    public void testIf17(){
        String actualStr = "IF(node(\"a\"),node(\"b\"),node(\"c\")).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(6)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").id("this is a id").tag("this is a tag").maxWaitSeconds(6).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出
    @Test
    public void testIf18(){
        String actualStr = "IF(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tnode(\"c\")\n).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(6)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").id("this is a id").tag("this is a tag").maxWaitSeconds(6).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // data map 测试
    @Test
    public void testIf19(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "ifData = '{\"name\":\"zhangsan\",\"age\":18}';\nIF(node(\"a\"),node(\"b\"),node(\"c\")).data(ifData)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").data("ifData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出
    @Test
    public void testIf20(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "ifData = '{\"name\":\"zhangsan\",\"age\":18}';\nIF(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tnode(\"c\")\n).data(ifData)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").data("ifData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // data JsonStr 测试
    @Test
    public void testIf21(){
        String actualStr = "ifData = '{\"name\":\"zhangsan\",\"age\":18}';\nIF(node(\"a\"),node(\"b\"),node(\"c\")).data(ifData)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").data("ifData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出
    @Test
    public void testIf22(){
        String actualStr = "ifData = '{\"name\":\"zhangsan\",\"age\":18}';\nIF(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tnode(\"c\")\n).data(ifData)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").data("ifData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(true),
                actualStr);
        System.out.println(actualStr);
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
    // data Bean 测试
    @Test
    public void testIf23(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String actualStr = "ifData = '{\"name\":\"zhangsan\",\"age\":18}';\nIF(node(\"a\"),node(\"b\"),node(\"c\")).data(ifData)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").data("ifData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出
    @Test
    public void testIf24(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String actualStr = "ifData = '{\"name\":\"zhangsan\",\"age\":18}';\nIF(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tnode(\"c\")\n).data(ifData)";
        Assertions.assertEquals(ELBus.ifOpt("a", "b", "c").data("ifData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

}
