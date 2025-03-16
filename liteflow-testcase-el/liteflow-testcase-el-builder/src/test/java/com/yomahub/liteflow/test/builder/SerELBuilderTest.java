package com.yomahub.liteflow.test.builder;

import cn.hutool.core.util.EscapeUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.util.JsonUtil;
import org.apache.commons.lang.StringEscapeUtils;
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
 * @author luo yi
 * @since 2.11.1
 */
@SpringBootTest(classes = SerELBuilderTest.class)
@EnableAutoConfiguration
public class SerELBuilderTest extends BaseTest {

    // then组件测试
    @Test
    public void testSer1(){
        String el = ELBus.ser("a", "b").toEL();
        Assertions.assertEquals("SER(a,b);",  el);
    }
    // 格式化输出测试
    @Test
    public void testSer2(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.element("b")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        Assertions.assertEquals("SER(\n\ta,\n\tb\n);", el);
    }
    // then组件then方法调用测试
    @Test
    public void testSer3(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.element("b")).ser(ELBus.element("c")).toEL();
        System.out.println(el);
        Assertions.assertEquals("SER(a,b,c);", el);
    }
    // 格式化输出测试
    @Test
    public void testSer4(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.element("b")).ser(ELBus.element("c")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        Assertions.assertEquals("SER(\n\ta,\n\tb,\n\tc\n);", el);
    }
    // then组件嵌套调用测试
    @Test
    public void testSer5(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c"))).ser(ELBus.element("d")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        Assertions.assertEquals("SER(a,SER(b,c),d);", el);
    }
    // 格式化输出测试
    @Test
    public void testSer6(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c"))).ser(ELBus.element("d")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t),\n\td\n);";
        Assertions.assertEquals(expect, el);
    }
    // pre组件测试
    @Test
    public void testSer7(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c"))).ser(ELBus.element("d")).pre(ELBus.element("p")).pre(ELBus.element("pp")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(PRE(p),PRE(pp),a,SER(b,c),d);";
        Assertions.assertEquals(expect, el);
    }
    // 格式化输出测试
    @Test
    public void testSer8(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c"))).ser(ELBus.element("d")).pre(ELBus.element("p")).pre(ELBus.element("pp")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(\n\tPRE(\n\t\tp\n\t),\n\tPRE(\n\t\tpp\n\t),\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t),\n\td\n);";
        Assertions.assertEquals(expect, el);
    }
    // pre finally 格式测试
    @Test
    public void testSer9(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c"))).ser(ELBus.element("d")).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(PRE(p),a,SER(b,c),d,FINALLY(f));";
        Assertions.assertEquals(expect, el);
    }
    // 格式化输出测试
    @Test
    public void testSer10(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c"))).ser(ELBus.element("d")).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(\n\tPRE(\n\t\tp\n\t),\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t),\n\td,\n\tFINALLY(\n\t\tf\n\t)\n);";
        Assertions.assertEquals(expect, el);
    }
    // 属性设置测试
    @Test
    public void testSer11(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d")).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(PRE(p),a,SER(b,c).id(\"this is a id\"),d,FINALLY(f)).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // 格式化输出测试
    @Test
    public void testSer12(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d")).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(\n\tPRE(\n\t\tp\n\t),\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t).id(\"this is a id\"),\n\td,\n\tFINALLY(\n\t\tf\n\t)\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // data属性测试
    @Test
    public void testSer13(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d").data("thenData", name2Value)).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL();
        System.out.println(el);
        String expect = "thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nSER(PRE(p),a,SER(b,c).id(\"this is a id\"),d.data(thenData),FINALLY(f)).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // 格式化输出测试
    @Test
    public void testSer14(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d").data("thenData", name2Value)).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nSER(\n\tPRE(\n\t\tp\n\t),\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t).id(\"this is a id\"),\n\td.data(thenData),\n\tFINALLY(\n\t\tf\n\t)\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // data属性测试 Json字符串赋值data
    @Test
    public void testSer15(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nSER(PRE(p),a,SER(b,c).id(\"this is a id\"),d.data(thenData),FINALLY(f)).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // 格式化输出测试 Json字符串赋值data
    @Test
    public void testSer16(){
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d").data("thenData", "{\"name\":\"zhangsan\",\"age\":18}")).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nSER(\n\tPRE(\n\t\tp\n\t),\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t).id(\"this is a id\"),\n\td.data(thenData),\n\tFINALLY(\n\t\tf\n\t)\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
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
        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d").data("thenData", name2Value)).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nSER(PRE(p),a,SER(b,c).id(\"this is a id\"),d.data(thenData),FINALLY(f)).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // 格式化输出测试
    @Test
    public void testSer18(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;

        String el = ELBus.ser(ELBus.element("a"), ELBus.ser(ELBus.element("b")).ser(ELBus.element("c")).id("this is a id")).tag("this is a tag").ser(ELBus.element("d").data("thenData", name2Value)).pre(ELBus.element("p")).finallyOpt(ELBus.element("f")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "thenData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nSER(\n\tPRE(\n\t\tp\n\t),\n\ta,\n\tSER(\n\t\tb,\n\t\tc\n\t).id(\"this is a id\"),\n\td.data(thenData),\n\tFINALLY(\n\t\tf\n\t)\n).tag(\"this is a tag\");";
        Assertions.assertEquals(expect, el);
    }
    // maxWaitSecond测试
    @Test
    public void testSer19(){
        String el = ELBus.ser(ELBus.element("a")).ser(ELBus.element("b")).maxWaitSeconds(5).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(a,b).maxWaitSeconds(5);";
        Assertions.assertEquals(expect, el);

    }
    // 格式化输出测试
    @Test
    public void testSer20(){
        String el = ELBus.ser(ELBus.element("a")).ser(ELBus.element("b")).maxWaitSeconds(5).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "SER(\n\ta,\n\tb\n).maxWaitSeconds(5);";
        Assertions.assertEquals(expect, el);
    }
}
