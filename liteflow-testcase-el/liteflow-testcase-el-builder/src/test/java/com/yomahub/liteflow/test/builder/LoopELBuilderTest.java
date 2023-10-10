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
 * 循环组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = LoopELBuilderTest.class)
@EnableAutoConfiguration
public class LoopELBuilderTest extends BaseTest {
    // for 限定次数循环
    @Test
    public void testLoop1(){
        String actualStr = "FOR(3).DO(THEN(node(\"a\"),node(\"b\"),node(\"c\"))).BREAK(node(\"d\"))";
        Assertions.assertEquals(ELBus.forOpt(3).doOpt(ELBus.then("a", "b", "c")).breakOpt("d").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出
    @Test
    public void testLoop2(){
        String actualStr = "FOR(3).DO(\n\tTHEN(\n\t\tnode(\"a\"),\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).BREAK(\n\tnode(\"d\")\n)";
        Assertions.assertEquals(ELBus.forOpt(3).doOpt(ELBus.then("a", "b", "c")).breakOpt("d").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // for 单节点循环测试
    @Test
    public void testLoop3(){
        String actualStr = "FOR(node(\"a\")).DO(WHEN(node(\"b\"),node(\"c\"),node(\"d\"))).BREAK(AND(node(\"e\"),node(\"f\")))";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.when("b", "c", "d")).breakOpt(ELBus.and("e", "f")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop4(){
        String actualStr = "FOR(\n\tnode(\"a\")\n).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t)\n).BREAK(\n\tAND(\n\t\tnode(\"e\"),\n\t\tnode(\"f\")\n\t)\n)";
        Assertions.assertEquals(ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.when("b", "c", "d")).breakOpt(ELBus.and("e", "f")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // parallel语句测试
    @Test
    public void testLoop5(){
        String actualStr = "FOR(node(\"a\")).parallel(true).DO(WHEN(node(\"b\"),node(\"c\"),node(\"d\"))).BREAK(node(\"e\"))";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.when("b", "c", "d")).breakOpt("e").parallel(true).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop6(){
        String actualStr = "FOR(\n\tnode(\"a\")\n).parallel(true).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t)\n).BREAK(\n\tnode(\"e\")\n)";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.when("b", "c", "d")).breakOpt("e").parallel(true).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // 属性测试
    @Test
    public void testLoop7(){
        String actualStr = "forData = '{\"name\":\"zhangsan\",\"age\":18}';\nFOR(node(\"a\")).DO(WHEN(node(\"b\"),node(\"c\"),node(\"d\"))).BREAK(node(\"e\")).id(\"this is a id\").tag(\"this is a tag\").data(forData).maxWaitSeconds(3)";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.when("b", "c", "d")).breakOpt("e").id("this is a id").tag("this is a tag").maxWaitSeconds(3).data("forData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop8(){
        String actualStr = "forData = '{\"name\":\"zhangsan\",\"age\":18}';\nFOR(\n\tnode(\"a\")\n).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t)\n).BREAK(\n\tnode(\"e\")\n).id(\"this is a id\").tag(\"this is a tag\").data(forData).maxWaitSeconds(3)";
        Assertions.assertEquals(ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.when("b", "c", "d")).breakOpt("e").id("this is a id").tag("this is a tag").maxWaitSeconds(3).data("forData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // while调用测试
    @Test
    public void testLoop9(){
        String actualStr = "WHILE(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).BREAK(node(\"f\"))";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).breakOpt("f").toEL(),
                actualStr);
        Assertions.assertEquals(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then("b", "c")).breakOpt("f").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "WHILE(AND(node(\"a\"),node(\"b\"))).DO(node(\"c\")).BREAK(node(\"d\"))";
        Assertions.assertEquals(ELBus.whileOpt(ELBus.and("a", "b")).doOpt("c").breakOpt("d").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "WHILE(OR(node(\"a\"),node(\"b\"))).DO(node(\"c\")).BREAK(node(\"d\"))";
        Assertions.assertEquals(ELBus.whileOpt(ELBus.or("a", "b")).doOpt("c").breakOpt("d").toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "WHILE(NOT(node(\"a\"))).DO(node(\"c\")).BREAK(node(\"d\"))";
        Assertions.assertEquals(ELBus.whileOpt(ELBus.not("a")).doOpt("c").breakOpt("d").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop10(){
        String actualStr = "WHILE(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).BREAK(\n\tnode(\"f\")\n)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).breakOpt("f").toEL(true),
                actualStr);
        Assertions.assertEquals(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then("b", "c")).breakOpt("f").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "WHILE(\n\tAND(\n\t\tnode(\"a\"),\n\t\tnode(\"b\")\n\t)\n).DO(\n\tnode(\"c\")\n).BREAK(\n\tnode(\"d\")\n)";
        Assertions.assertEquals(ELBus.whileOpt(ELBus.and("a", "b")).doOpt("c").breakOpt("d").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "WHILE(\n\tOR(\n\t\tnode(\"a\"),\n\t\tnode(\"b\")\n\t)\n).DO(\n\tnode(\"c\")\n).BREAK(\n\tnode(\"d\")\n)";
        Assertions.assertEquals(ELBus.whileOpt(ELBus.or("a", "b")).doOpt("c").breakOpt("d").toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "WHILE(\n\tNOT(\n\t\tnode(\"a\")\n\t)\n).DO(\n\tnode(\"c\")\n).BREAK(\n\tnode(\"d\")\n)";
        Assertions.assertEquals(ELBus.whileOpt(ELBus.not("a")).doOpt("c").breakOpt("d").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // while属性调用测试
    @Test
    public void testLoop11(){
        String actualStr = "whileData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHILE(node(\"a\")).parallel(true).DO(THEN(node(\"b\"),node(\"c\"))).BREAK(node(\"d\")).id(\"this is a ig\").tag(\"this is a tag\").data(whileData).maxWaitSeconds(3)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).breakOpt("d").id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).data("whileData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop12(){
        String actualStr = "whileData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHILE(\n\tnode(\"a\")\n).parallel(true).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).BREAK(\n\tnode(\"d\")\n).id(\"this is a ig\").tag(\"this is a tag\").data(whileData).maxWaitSeconds(3)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).breakOpt("d").id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).data("whileData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // Iterator 调用测试
    @Test
    public void testLoop13(){
        String actualStr = "ITERATOR(node(\"a\")).DO(WHEN(node(\"b\"),node(\"c\")))";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.when("b", "c")).toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "ITERATOR(node(\"a\")).DO(WHEN(node(\"b\"),node(\"c\")))";
        Assertions.assertEquals(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when("b", "c")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop14(){
        String actualStr = "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.when("b", "c")).toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n)";
        Assertions.assertEquals(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when("b", "c")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // iterator 属性测试
    @Test
    public void testLoop15(){
        String actualStr = "iteratorData = '{\"name\":\"zhangsan\",\"age\":18}';\nITERATOR(node(\"a\")).parallel(true).DO(THEN(node(\"b\"),node(\"c\"))).id(\"this is a ig\").tag(\"this is a tag\").data(iteratorData).maxWaitSeconds(3)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.then("b", "c")).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).data("iteratorData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    @Test
    public void testLoop16(){
        String actualStr = "iteratorData = '{\"name\":\"zhangsan\",\"age\":18}';\nITERATOR(\n\tnode(\"a\")\n).parallel(true).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).id(\"this is a ig\").tag(\"this is a tag\").data(iteratorData).maxWaitSeconds(3)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.then("b", "c")).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).data("iteratorData", "{\"name\":\"zhangsan\",\"age\":18}").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // data Map 参数 测试
    @Test
    public void testLoop17(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "forData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "FOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).data(forData)";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.then("b", "c")).data("forData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "whileData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "WHILE(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).data(whileData)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).data("whileData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "iteratorData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "ITERATOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).data(iteratorData)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.then("b", "c")).data("iteratorData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    @Test
    public void testLoop18(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "forData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "FOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).data(forData)";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.then("b", "c")).data("forData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "whileData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "WHILE(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).data(whileData)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).data("whileData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "iteratorData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).data(iteratorData)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.then("b", "c")).data("iteratorData", name2Value).toEL(true),
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
    // data JavaBean参数 测试
    @Test
    public void testLoop19(){
        ParamClass name2Value = new ParamClass();
        name2Value.age = 18;
        name2Value.name = "zhangsan";
        String actualStr = "forData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "FOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).data(forData)";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.then("b", "c")).data("forData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "whileData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "WHILE(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).data(whileData)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).data("whileData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
        actualStr = "iteratorData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "ITERATOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).data(iteratorData)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.then("b", "c")).data("iteratorData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    @Test
    public void testLoop20(){
        ParamClass name2Value = new ParamClass();
        name2Value.age = 18;
        name2Value.name = "zhangsan";
        String actualStr = "forData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "FOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).data(forData)";
        Assertions.assertEquals(ELBus.forOpt("a").doOpt(ELBus.then("b", "c")).data("forData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "whileData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "WHILE(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).data(whileData)";
        Assertions.assertEquals(ELBus.whileOpt("a").doOpt(ELBus.then("b", "c")).data("whileData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
        actualStr = "iteratorData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).data(iteratorData)";
        Assertions.assertEquals(ELBus.iteratorOpt("a").doOpt(ELBus.then("b", "c")).data("iteratorData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

}
