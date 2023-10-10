package com.yomahub.liteflow.test.builder;

import cn.hutool.json.JSONUtil;
import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.test.BaseTest;
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
@SpringBootTest(classes = WhenELBuilderTest.class)
@EnableAutoConfiguration
public class WhenELBuilderTest extends BaseTest {
    // then组件测试
    @Test
    public void testWhen1(){
        String actualStr = "WHEN(node(\"a\"),node(\"b\"))";
        Assertions.assertEquals(ELBus.when("a", "b").toEL(), actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen2(){
        String actualStr = "WHEN(\n\tnode(\"a\"),\n\tnode(\"b\")\n)";
        Assertions.assertEquals(ELBus.when("a", "b").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // then组件then方法调用测试
    @Test
    public void testWhen3(){
        String actualStr = "WHEN(node(\"a\"),node(\"b\"),node(\"c\"))";
        Assertions.assertEquals(ELBus.when("a", "b").when("c").toEL(),
                actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen4(){
        String actualStr = "WHEN(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tnode(\"c\")\n)";
        Assertions.assertEquals(ELBus.when("a", "b").when("c").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // then组件嵌套调用测试
    @Test
    public void testWhen5(){
        String actualStr = "WHEN(node(\"a\"),WHEN(node(\"b\"),node(\"c\")),node(\"d\"))";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c")).when("d").toEL(),
                actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen6(){
        String actualStr = "WHEN(\n\tnode(\"a\"),\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t),\n\tnode(\"d\")\n)";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c")).when("d").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // WHEN特有属性测试 any ignoreError customThreadExecutor must
    @Test
    public void testWhen7(){
        String actualStr = "WHEN(node(\"a\"),node(\"b\"),WHEN(node(\"c\"),node(\"d\")).any(true).threadPool(\"WhenELBuilderTest.customThreadPool\").id(\"node1\")).ignoreError(true).must(\"a\", \"task1\", \"node1\")";
        Assertions.assertEquals(ELBus.when("a", "b",ELBus.when("c").when("d").customThreadExecutor("WhenELBuilderTest.customThreadPool").id("node1").any(true)).ignoreError(true).must("a", "task1", "node1").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen8(){
        String actualStr = "WHEN(\n\tnode(\"a\"),\n\tnode(\"b\"),\n\tWHEN(\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t).any(true).threadPool(\"WhenELBuilderTest.customThreadPool\").id(\"node1\")\n).ignoreError(true).must(\"a\", \"task1\", \"node1\")";
        Assertions.assertEquals(ELBus.when("a", "b",ELBus.when("c").when("d").customThreadExecutor("WhenELBuilderTest.customThreadPool").id("node1").any(true)).ignoreError(true).must("a", "task1", "node1").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // maxWaitSeconds 属性测试
    @Test
    public void testWhen9(){
        String actualStr = "WHEN(node(\"a\"),node(\"b\")).maxWaitSeconds(5)";
        Assertions.assertEquals(ELBus.when("a", "b").maxWaitSeconds(5).toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen10(){
        String actualStr = "WHEN(\n\tnode(\"a\"),\n\tnode(\"b\")\n).maxWaitSeconds(5)";
        Assertions.assertEquals(ELBus.when("a", "b").maxWaitSeconds(5).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // 属性设置测试
    @Test
    public void testWhen11(){
        String actualStr = "WHEN(node(\"a\"),WHEN(node(\"b\"),node(\"c\")).id(\"this is a id\"),node(\"d\")).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").id("this is a id")).when("d").tag("this is a tag").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen12(){
        String actualStr = "WHEN(\n\tnode(\"a\"),\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\"),\n\tnode(\"d\")\n).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").id("this is a id")).when("d").tag("this is a tag").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // data属性测试
    @Test
    public void testWhen13(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        System.out.println(JSONUtil.toJsonStr(name2Value));
        String actualStr = "whenData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHEN(node(\"a\"),WHEN(node(\"b\"),node(\"c\")).id(\"this is a id\").data(whenData),node(\"d\")).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").data("whenData", name2Value).id("this is a id")).when("d").tag("this is a tag").toEL(false),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen14(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "whenData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHEN(\n\tnode(\"a\"),\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\").data(whenData),\n\tnode(\"d\")\n).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").data("whenData", name2Value).id("this is a id")).when("d").tag("this is a tag").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
    // data属性测试 Json字符串赋值data
    @Test
    public void testWhen15(){
        String actualStr = "whenData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHEN(node(\"a\"),WHEN(node(\"b\"),node(\"c\")).id(\"this is a id\").data(whenData),node(\"d\")).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").data("whenData", "{\"name\":\"zhangsan\",\"age\":18}").id("this is a id")).when("d").tag("this is a tag").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试 Json字符串赋值data
    @Test
    public void testWhen16(){
        String actualStr = "whenData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHEN(\n\tnode(\"a\"),\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\").data(whenData),\n\tnode(\"d\")\n).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").data("whenData", "{\"name\":\"zhangsan\",\"age\":18}").id("this is a id")).when("d").tag("this is a tag").toEL(true),
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
    // data属性测试
    @Test
    public void testWhen17(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String actualStr = "whenData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHEN(node(\"a\"),WHEN(node(\"b\"),node(\"c\")).id(\"this is a id\").data(whenData),node(\"d\")).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").data("whenData", name2Value).id("this is a id")).when("d").tag("this is a tag").toEL(),
                actualStr);
        System.out.println(actualStr);
    }
    // 格式化输出测试
    @Test
    public void testWhen18(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String actualStr = "whenData = '{\"name\":\"zhangsan\",\"age\":18}';\nWHEN(\n\tnode(\"a\"),\n\tWHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t).id(\"this is a id\").data(whenData),\n\tnode(\"d\")\n).tag(\"this is a tag\")";
        Assertions.assertEquals(ELBus.when("a", ELBus.when("b").when("c").data("whenData", name2Value).id("this is a id")).when("d").tag("this is a tag").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }
}
