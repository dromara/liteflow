package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.el.ParELWrapper;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * 并行组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = ParELBuilderTest.class)
@EnableAutoConfiguration
public class ParELBuilderTest extends BaseTest {
    // then组件测试
    @Test
    public void testPar1(){
        String expectedStr = "PAR(node(\"a\"),node(\"b\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar2(){
        String expectedStr = "PAR(\n\tnode(\"a\"),\n\tnode(\"b\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b")).toEL(true)));
    }
    // then组件then方法调用测试
    @Test
    public void testPar3(){
        String expectedStr = "PAR(node(\"a\"),node(\"b\"),node(\"c\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b")).par(ELBus.node("c")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b")).par(ELBus.node("c")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar4(){
        String expectedStr = "PAR(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tnode(\"c\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b")).par(ELBus.node("c")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b")).par(ELBus.node("c")).toEL(true)));
    }
    // then组件嵌套调用测试
    @Test
    public void testPar5(){
        String expectedStr = "PAR(node(\"a\"),PAR(node(\"b\"),node(\"c\")),node(\"d\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c"))).par(ELBus.node("d")).toEL());
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c"))).par(ELBus.node("d")).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar6(){
        String expectedStr = "PAR(\n\tnode(\"a\"),\n\tPAR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c"))).par(ELBus.node("d")).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c"))).par(ELBus.node("d")).toEL(true)));
    }
    // PAR特有属性测试 any ignoreError customThreadExecutor must
    @Test
    public void testPar7(){
        String expectedStr = "PAR(node(\"a\"),node(\"b\"),PAR(node(\"c\"),node(\"d\")).any(true).threadPool(\"com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1\").id(\"node1\")).ignoreError(true).must(\"a\", \"task1\", \"node1\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b"), ELBus.par(ELBus.node("c")).par(ELBus.node("d")).id("node1").customThreadExecutor("com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1").any(true)).ignoreError(true).must("a","task1","node1").toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b"), ELBus.par(ELBus.node("c")).par(ELBus.node("d")).id("node1").customThreadExecutor("com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1").any(true)).ignoreError(true).must("a","task1","node1").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar8(){
        String expectedStr = "PAR(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tPAR(\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t).any(true).threadPool(\"com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1\").id(\"node1\")\n).ignoreError(true).must(\"a\", \"task1\", \"node1\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b"), ELBus.par(ELBus.node("c")).par(ELBus.node("d")).customThreadExecutor("com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1").id("node1").any(true)).ignoreError(true).must("a","task1","node1").toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b"), ELBus.par(ELBus.node("c")).par(ELBus.node("d")).customThreadExecutor("com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1").id("node1").any(true)).ignoreError(true).must("a","task1","node1").toEL(true)));
    }
    // maxWaitSeconds 属性测试
    @Test
    public void testPar9(){
        String expectedStr = "PAR(node(\"a\"),node(\"b\")).maxWaitSeconds(5);";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b")).maxWaitSeconds(5).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b")).maxWaitSeconds(5).toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar10(){
        String expectedStr = "PAR(\n\tnode(\"a\"),\n\tnode(\"b\")\n).maxWaitSeconds(5);";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.node("b")).maxWaitSeconds(5).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.node("b")).maxWaitSeconds(5).toEL(true)));
    }
    // 属性设置测试
    @Test
    public void testPar11(){
        String expectedStr = "PAR(node(\"a\"),PAR(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\")).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar12(){
        String expectedStr = "PAR(\n\tnode(\"a\"),\n\tPAR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\")\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true)));
    }
    // data属性测试
    @Test
    public void testPar13(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        System.out.println(JsonUtil.toJsonString(name2Value));
        String expectedStr = "whenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nPAR(node(\"a\"),PAR(node(\"b\"),node(\"c\").data(whenData)).id(\"this is a id\"),node(\"d\")).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(false));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(false)));
    }
    // 格式化输出测试
    @Test
    public void testPar14(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "whenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nPAR(\n\tnode(\"a\"),\n\tPAR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(whenData)\n\t).id(\"this is a id\"),\n\tnode(\"d\")\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true)));
    }
    // data属性测试 Json字符串赋值data
    @Test
    public void testPar15(){
        String expectedStr = "whenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nPAR(node(\"a\"),PAR(node(\"b\"),node(\"c\").data(whenData)).id(\"this is a id\"),node(\"d\")).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL()));
    }
    // 格式化输出测试 Json字符串赋值data
    @Test
    public void testPar16(){
        String expectedStr = "whenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nPAR(\n\tnode(\"a\"),\n\tPAR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(whenData)\n\t).id(\"this is a id\"),\n\tnode(\"d\")\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", "{\"name\":\"zhangsan\",\"age\":18}")).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true)));
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
    public void testPar17(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String expectedStr = "whenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nPAR(node(\"a\"),PAR(node(\"b\"),node(\"c\").data(whenData)).id(\"this is a id\"),node(\"d\")).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL()));
    }
    // 格式化输出测试
    @Test
    public void testPar18(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String expectedStr = "whenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nPAR(\n\tnode(\"a\"),\n\tPAR(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(whenData)\n\t).id(\"this is a id\"),\n\tnode(\"d\")\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expectedStr,
                ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.par(ELBus.node("a"), ELBus.par(ELBus.node("b")).par(ELBus.node("c").data("whenData", name2Value)).id("this is a id")).par(ELBus.node("d")).tag("this is a tag").toEL(true)));
    }

    @Test
    public void testPAR(){
        ParELWrapper el = ELBus.par(ELBus.node("a"), ELBus.node("b"), ELBus.node("c")).customThreadExecutor("com.yomahub.liteflow.test.builder.customTreadExecutor.CustomThreadExecutor1");
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(el.toEL()));
    }
}
