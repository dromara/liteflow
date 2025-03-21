package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.test.BaseTest;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * 捕获异常组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = CatchELBuilderTest.class)
@EnableAutoConfiguration
public class CatchELBuilderTest extends BaseTest {
    // catch捕获异常调用测试
    @Test
    public void testCatch1(){
        String el = ELBus.catchException(ELBus.then(ELBus.element("a"), ELBus.element("b"))).doOpt(ELBus.element("c")).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "CATCH(THEN(a,b)).DO(c);";
        Assertions.assertEquals(expect, el);
    }
    @Test
    public void testCatch2(){
        String el = ELBus.catchException(ELBus.then(ELBus.element("a"), ELBus.element("b"))).doOpt(ELBus.element("c")).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "CATCH(\n\tTHEN(\n\t\ta,\n\t\tb\n\t)\n).DO(\n\tc\n);";
        Assertions.assertEquals(expect, el);
    }
    // 属性设置测试
    @Test
    public void testCatch3(){
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c"))).id("this is a id").tag("this is a tag").maxWaitSeconds(3).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "CATCH(a).DO(THEN(b,c)).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expect, el);
    }

    @Test
    public void testCatch4(){
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c"))).id("this is a id").tag("this is a tag").maxWaitSeconds(3).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "CATCH(\n\ta\n).DO(\n\tTHEN(\n\t\tb,\n\t\tc\n\t)\n).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expect, el);
    }
    // data 设置 jsonStr
    @Test
    public void testCatch5(){
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c").data("catchData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "catchData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nCATCH(a).DO(THEN(b,c.data(catchData)));";
        Assertions.assertEquals(expect, el);
    }
    @Test
    public void testCatch6(){
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c").data("catchData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "catchData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nCATCH(\n\ta\n).DO(\n\tTHEN(\n\t\tb,\n\t\tc.data(catchData)\n\t)\n);";
        Assertions.assertEquals(expect, el);
   }
    // data 设置 map
    @Test
    public void testCatch7(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c").data("catchData", name2Value))).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "catchData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nCATCH(a).DO(THEN(b,c.data(catchData)));";
        Assertions.assertEquals(expect, el);
    }

    @Test
    public void testCatch8(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c").data("catchData", name2Value))).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "catchData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nCATCH(\n\ta\n).DO(\n\tTHEN(\n\t\tb,\n\t\tc.data(catchData)\n\t)\n);";
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
    // data 设置 bean
    @Test
    public void testCatch9(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c").data("catchData", name2Value))).toEL();
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "catchData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nCATCH(a).DO(THEN(b,c.data(catchData)));";
        Assertions.assertEquals(expect, el);
    }

    @Test
    public void testCatch10(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String el = ELBus.catchException(ELBus.element("a")).doOpt(ELBus.then(ELBus.element("b"), ELBus.element("c").data("catchData", name2Value))).toEL(true);
        System.out.println(StringEscapeUtils.escapeJava(el));
        String expect = "catchData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\nCATCH(\n\ta\n).DO(\n\tTHEN(\n\t\tb,\n\t\tc.data(catchData)\n\t)\n);";
        Assertions.assertEquals(expect, el);
    }
}
