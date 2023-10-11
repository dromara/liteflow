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
 * 选择组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = SwitchELBuilderTest.class)
@EnableAutoConfiguration
public class SwitchELBuilderTest extends BaseTest {

    // Switch调用方法测试
    @Test
    public void testSwitch1(){
        String actualStr = "SWITCH(node(\"a\")).TO(node(\"b\"),node(\"c\"),node(\"d\")).DEFAULT(node(\"f\"))";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").defaultOpt("f").toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    // 格式化输出测试
    @Test
    public void testSwitch2(){
        String actualStr = "SWITCH(node(\"a\")).TO(\n\tnode(\"b\"),\n\tnode(\"c\"),\n\tnode(\"d\")\n).DEFAULT(\n\tnode(\"f\")\n)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").defaultOpt("f").toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

    // switch和THEN when嵌套调用测试
    @Test
    public void testSwitch3(){
        String actualStr = "SWITCH(node(\"a\")).TO(node(\"b\"),THEN(node(\"c\"),node(\"d\")),WHEN(node(\"e\"),node(\"f\"))).DEFAULT(THEN(node(\"g\"),node(\"h\")))";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", ELBus.then("c", "d"), ELBus.when("e", "f")).defaultOpt(ELBus.then("g", "h")).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    // 格式化输出测试
    @Test
    public void testSwitch4(){
        String actualStr = "SWITCH(node(\"a\")).TO(\n\tnode(\"b\"),\n\tTHEN(\n\t\tnode(\"c\"),\n\t\tnode(\"d\")\n\t),\n\tWHEN(\n\t\tnode(\"e\"),\n\t\tnode(\"f\")\n\t)\n).DEFAULT(\n\tTHEN(\n\t\tnode(\"g\"),\n\t\tnode(\"h\")\n\t)\n)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", ELBus.then("c", "d"), ELBus.when("e", "f")).defaultOpt(ELBus.then("g", "h")).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

    // 属性设置测试
    @Test
    public void testSwitch5(){
        String actualStr = "SWITCH(node(\"a\")).TO(node(\"b\"),node(\"c\"),node(\"d\")).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(5)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").id("this is a id").tag("this is a tag").maxWaitSeconds(5).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    // 格式化输出测试
    @Test
    public void testSwitch6(){
        String actualStr = "SWITCH(node(\"a\")).TO(\n\tnode(\"b\"),\n\tnode(\"c\"),\n\tnode(\"d\")\n).id(\"this is a id\").tag(\"this is a tag\").maxWaitSeconds(5)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").id("this is a id").tag("this is a tag").maxWaitSeconds(5).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

    // data属性测试
    @Test
    public void testSwitch7(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "switchData = '{\"name\":\"zhangsan\",\"age\":18}';\nSWITCH(node(\"a\")).TO(node(\"b\"),node(\"c\"),node(\"d\")).data(switchData)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").data("switchData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    @Test
    public void testSwitch8(){
        Map<String, Object> name2Value = new HashMap<String, Object>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String actualStr = "switchData = '{\"name\":\"zhangsan\",\"age\":18}';\nSWITCH(node(\"a\")).TO(\n\tnode(\"b\"),\n\tnode(\"c\"),\n\tnode(\"d\")\n).data(switchData)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").data("switchData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

    @Test
    public void testSwitch9(){
        String jsonStr = "{\"name\":\"zhangsan\",\"age\":18}";
        String actualStr = "switchData = '{\"name\":\"zhangsan\",\"age\":18}';\nSWITCH(node(\"a\")).TO(node(\"b\"),node(\"c\"),node(\"d\")).data(switchData)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").data("switchData", jsonStr).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    @Test
    public void testSwitch10(){
        String jsonStr = "{\"name\":\"zhangsan\",\"age\":18}";
        String actualStr = "switchData = '{\"name\":\"zhangsan\",\"age\":18}';\nSWITCH(node(\"a\")).TO(\n\tnode(\"b\"),\n\tnode(\"c\"),\n\tnode(\"d\")\n).data(switchData)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").data("switchData", jsonStr).toEL(true),
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

    @Test
    public void testSwitch11(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String actualStr = "switchData = '{\"name\":\"zhangsan\",\"age\":18}';\nSWITCH(node(\"a\")).TO(node(\"b\"),node(\"c\"),node(\"d\")).data(switchData)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").data("switchData", name2Value).toEL(),
                actualStr);
        System.out.println(actualStr);
    }

    @Test
    public void testSwitch12(){
        ParamClass name2Value = new ParamClass();
        name2Value.name = "zhangsan";
        name2Value.age = 18;
        String actualStr = "switchData = '{\"name\":\"zhangsan\",\"age\":18}';\nSWITCH(node(\"a\")).TO(\n\tnode(\"b\"),\n\tnode(\"c\"),\n\tnode(\"d\")\n).data(switchData)";
        Assertions.assertEquals(ELBus.switchOpt("a").to("b", "c", "d").data("switchData", name2Value).toEL(true),
                actualStr);
        System.out.println(actualStr);
    }

}
