package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.test.BaseTest;
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
        String expectedStr = "CATCH(THEN(node(\"a\"),node(\"b\"))).DO(node(\"c\"));";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException(ELBus.then("a", "b")).doOpt("c").toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException(ELBus.then("a", "b")).doOpt("c").toEL()));
    }
    @Test
    public void testCatch2(){
        String expectedStr = "CATCH(\n\tTHEN(\n\t\tnode(\"a\"),\n\t\tnode(\"b\")\n\t)\n).DO(\n\tnode(\"c\")\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException(ELBus.then("a", "b")).doOpt("c").toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException(ELBus.then("a", "b")).doOpt("c").toEL(true)));
    }
    // 属性设置测试
    @Test
    public void testCatch3(){
        String expectedStr = "CATCH(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\"))).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", "c")).id("this is a id").tag("this is a tag").maxWaitSeconds(3).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", "c")).id("this is a id").tag("this is a tag").maxWaitSeconds(3).toEL()));
    }
    @Test
    public void testCatch4(){
        String expectedStr = "CATCH(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\")\n\t)\n).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(3);";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", "c")).id("this is a id").tag("this is a tag").maxWaitSeconds(3).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", "c")).id("this is a id").tag("this is a tag").maxWaitSeconds(3).toEL(true)));
    }
    // data 设置 jsonStr
    @Test
    public void testCatch5(){
        String expectedStr = "catchData = '{\"name\":\"zhangsan\",\"age\":18}';\nCATCH(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(catchData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL()));
    }
    @Test
    public void testCatch6(){
        String expectedStr = "catchData = '{\"name\":\"zhangsan\",\"age\":18}';\nCATCH(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(catchData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", "{\"name\":\"zhangsan\",\"age\":18}"))).toEL(true)));
   }
    // data 设置 map
    @Test
    public void testCatch7(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "catchData = '{\"name\":\"zhangsan\",\"age\":18}';\nCATCH(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(catchData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL()));
    }
    @Test
    public void testCatch8(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "catchData = '{\"name\":\"zhangsan\",\"age\":18}';\nCATCH(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(catchData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL(true)));
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
        String expectedStr = "catchData = '{\"name\":\"zhangsan\",\"age\":18}';\nCATCH(node(\"a\")).DO(THEN(node(\"b\"),node(\"c\").data(catchData)));";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL()));
    }
    @Test
    public void testCatch10(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String expectedStr = "catchData = '{\"name\":\"zhangsan\",\"age\":18}';\nCATCH(\n\tnode(\"a\")\n).DO(\n\tTHEN(\n\t\tnode(\"b\"),\n\t\tnode(\"c\").data(catchData)\n\t)\n);";
        Assertions.assertEquals(expectedStr,
                ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(ELBus.catchException("a").doOpt(ELBus.then("b", ELBus.node("c").data("catchData", name2Value))).toEL(true)));
    }
}
