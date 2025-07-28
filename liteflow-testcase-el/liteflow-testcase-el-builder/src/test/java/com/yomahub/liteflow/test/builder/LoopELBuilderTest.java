package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.el.LoopELWrapper;
import com.yomahub.liteflow.common.entity.ValidationResp;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.SelectiveJavaEscaper;
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
        LoopELWrapper loopELWrapper = ELBus.forOpt(3).doOpt(ELBus.then("a","b","c")).breakOpt("d");
        String el = loopELWrapper.toEL();
        String expectedStr1 = "FOR(3).DO(THEN(a,b,c)).BREAK(d);";
        Assertions.assertEquals(expectedStr1,el);

        String formattedEl = SelectiveJavaEscaper.escape(loopELWrapper.toEL(true));
        String expectedStr2 = "FOR(3).DO(\\n\\tTHEN(\\n\\t\\ta,\\n\\t\\tb,\\n\\t\\tc\\n\\t)\\n).BREAK(\\n\\td\\n);";
        Assertions.assertEquals(expectedStr2,formattedEl);
    }

    // for 单节点循环测试
    @Test
    public void testLoop2(){
        LoopELWrapper loopELWrapper = ELBus.forOpt("a").doOpt(ELBus.when("b","c","d")).breakOpt(ELBus.and("e","f"));
        String el = loopELWrapper.toEL();
        String expectedStr1 = "FOR(a).DO(WHEN(b,c,d)).BREAK(AND(e,f));";
        Assertions.assertEquals(expectedStr1,el);

        String formattedEl = SelectiveJavaEscaper.escape(loopELWrapper.toEL(true));
        String expectedStr2 = "FOR(\\n\\ta\\n).DO(\\n\\tWHEN(\\n\\t\\tb,\\n\\t\\tc,\\n\\t\\td\\n\\t)\\n).BREAK(\\n\\tAND(\\n\\t\\te,\\n\\t\\tf\\n\\t)\\n);";
        Assertions.assertEquals(expectedStr2,formattedEl);
    }

    // parallel语句测试
    @Test
    public void testLoop3(){
        LoopELWrapper loopELWrapper = ELBus.forOpt("a").doOpt(ELBus.when("b","c","d")).breakOpt("e").parallel(true);
        String el = loopELWrapper.toEL();
        String expectedStr1 = "FOR(a).parallel(true).DO(WHEN(b,c,d)).BREAK(e);";
        Assertions.assertEquals(expectedStr1,el);

        String formattedEl = SelectiveJavaEscaper.escape(loopELWrapper.toEL(true));
        String expectedStr2 = "FOR(\\n\\ta\\n).parallel(true).DO(\\n\\tWHEN(\\n\\t\\tb,\\n\\t\\tc,\\n\\t\\td\\n\\t)\\n).BREAK(\\n\\te\\n);";
        Assertions.assertEquals(expectedStr2,formattedEl);
    }

    // 属性测试
    @Test
    public void testLoop7(){
        LoopELWrapper loopELWrapper = ELBus.forOpt("a").doOpt(ELBus.when("b","c","d")).breakOpt(ELBus.element("e").data("forData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a id").tag("this is a tag").maxWaitSeconds(3);
        String el = loopELWrapper.toEL();
        String expectedStr1 = "forData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nFOR(a).DO(WHEN(b,c,d)).BREAK(e.data(forData)).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr1,el);

        String formattedEl = SelectiveJavaEscaper.escape(loopELWrapper.toEL(true));
        System.out.println(formattedEl);
        String expectedStr2 = "forData = \\\"{\\\\\\\"name\\\\\\\":\\\\\\\"zhangsan\\\\\\\",\\\\\\\"age\\\\\\\":18}\\\";\\nFOR(\\n\\ta\\n).DO(\\n\\tWHEN(\\n\\t\\tb,\\n\\t\\tc,\\n\\t\\td\\n\\t)\\n).BREAK(\\n\\te.data(forData)\\n).id(\\\"this is a id\\\").tag(\\\"this is a tag\\\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr2,formattedEl);
    }

    // while调用测试
    @Test
    public void testLoop9(){
        String expectedStr = "WHILE(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).BREAK(node(\"f\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("f")).toEL());
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("f")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("f")).toEL()));

        expectedStr = "WHILE(AND(node(\"a\"),node(\"b\"))).DO(node(\"c\")).BREAK(node(\"d\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.and(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.and(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL()));

        expectedStr = "WHILE(OR(node(\"a\"),node(\"b\"))).DO(node(\"c\")).BREAK(node(\"d\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.or(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.or(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL()));

        expectedStr = "WHILE(NOT(node(\"a\"))).DO(node(\"c\")).BREAK(node(\"d\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.not(ELBus.node("a"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.not(ELBus.node("a"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL()));
    }
    @Test
    public void testLoop10(){
        String expectedStr = "WHILE(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).BREAK(\n\tnode(\"f\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("f")).toEL(true));
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("f")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("f")).toEL(true)));

        expectedStr = "WHILE(\n\tAND(\n\t\tnode(\"a\"),\n\t\tnode(\"b\")\n\t)\n).DO(\n\tnode(\"c\")\n).BREAK(\n\tnode(\"d\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.and(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.and(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL(true)));

        expectedStr = "WHILE(\n\tOR(\n\t\tnode(\"a\"),\n\t\tnode(\"b\")\n\t)\n).DO(\n\tnode(\"c\")\n).BREAK(\n\tnode(\"d\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.or(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.or(ELBus.node("a"), ELBus.node("b"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL(true)));

        expectedStr = "WHILE(\n\tNOT(\n\t\tnode(\"a\")\n\t)\n).DO(\n\tnode(\"c\")\n).BREAK(\n\tnode(\"d\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.not(ELBus.node("a"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.not(ELBus.node("a"))).doOpt(ELBus.node("c")).breakOpt(ELBus.node("d")).toEL(true)));
    }
    // while属性调用测试
    @Test
    public void testLoop11(){
        String expectedStr = "whileData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nWHILE(node(\"a\")).parallel(true).DO(THEN(node(\"b\"),node(\"c\"))).BREAK(node(\"d\").data(whileData)).id(\"this is a ig\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("d").data("whileData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("d").data("whileData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL()));
    }
    @Test
    public void testLoop12(){
        String expectedStr = "whileData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nWHILE(\n\tnode(\"a\")\n).parallel(true).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).BREAK(\n\tnode(\"d\").data(whileData)\n).id(\"this is a ig\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("d").data("whileData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c"))).breakOpt(ELBus.node("d").data("whileData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL(true)));
    }
    // Iterator 调用测试
    @Test
    public void testLoop13(){
        String expectedStr = "ITERATOR(node(\"a\")).DO(WHEN(node(\"b\"),node(\"c\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL()));
        expectedStr = "ITERATOR(node(\"a\")).DO(WHEN(node(\"b\"),node(\"c\")));";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL()));
    }
    @Test
    public void testLoop14(){
        String expectedStr = "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL(true)));
        expectedStr = "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.when(ELBus.node("b"), ELBus.node("c"))).toEL(true)));
    }
    // iterator 属性测试
    @Test
    public void testLoop15(){
        String expectedStr = "iteratorData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nITERATOR(node(\"a\")).parallel(true).DO(THEN(node(\"b\"),node(\"c\").data(iteratorData))).id(\"this is a ig\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", "{\"name\":\"zhangsan\",\"age\":18}"))).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", "{\"name\":\"zhangsan\",\"age\":18}"))).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL()));
    }
    @Test
    public void testLoop16(){
        String expectedStr = "iteratorData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nITERATOR(\n\tnode(\"a\")\n).parallel(true).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(iteratorData)\n\t)\n).id(\"this is a ig\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", "{\"name\":\"zhangsan\",\"age\":18}"))).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", "{\"name\":\"zhangsan\",\"age\":18}"))).id("this is a ig").tag("this is a tag").maxWaitSeconds(3).parallel(true).toEL(true)));
    }
    // data Map 参数 测试
    @Test
    public void testLoop17(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "forData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "FOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(forData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL()));
        expectedStr = "whileData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "WHILE(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(whileData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL()));
        expectedStr = "iteratorData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "ITERATOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(iteratorData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL()));
    }

    @Test
    public void testLoop18(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "forData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "FOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(forData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL(true)));
        expectedStr = "whileData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "WHILE(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(whileData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL(true)));
        expectedStr = "iteratorData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(iteratorData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL(true)));
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
        String expectedStr = "forData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "FOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(forData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL()));
        expectedStr = "whileData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "WHILE(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(whileData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL()));
        expectedStr = "iteratorData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "ITERATOR(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(iteratorData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL()));
    }

    @Test
    public void testLoop20(){
        ParamClass name2Value = new ParamClass();
        name2Value.age = 18;
        name2Value.name = "zhangsan";
        String expectedStr = "forData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "FOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(forData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.forOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("forData", name2Value))).toEL(true)));
        expectedStr = "whileData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "WHILE(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(whileData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.whileOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("whileData", name2Value))).toEL(true)));
        expectedStr = "iteratorData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "ITERATOR(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(iteratorData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.iteratorOpt(ELBus.node("a")).doOpt(ELBus.then(ELBus.node("b"), ELBus.node("c").data("iteratorData", name2Value))).toEL(true)));
    }

}
