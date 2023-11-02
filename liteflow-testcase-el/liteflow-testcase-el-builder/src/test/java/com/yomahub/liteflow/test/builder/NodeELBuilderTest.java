package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.el.NodeELWrapper;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.HashMap;
import java.util.Map;

/**
 * 单节点组件测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = NodeELBuilderTest.class)
@EnableAutoConfiguration
public class NodeELBuilderTest extends BaseTest {
    @Test
    public void testNodeEL1(){
        String jsonStr = "'{\"name\":\"zhangsan\",\"age\":18}'";
        String expectedStr = "nodeData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        NodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", jsonStr);
        Assertions.assertEquals(expectedStr,
                node.toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL()));
    }
    @Test
    public void testNodeEL2(){
        String jsonStr = "'{\"name\":\"zhangsan\",\"age\":18}'";
        String expectedStr = "nodeData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        NodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", jsonStr);
        Assertions.assertEquals(expectedStr,
                node.toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL(true)));
    }
    @Test
    public void testNodeEL3(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "nodeData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        NodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
        Assertions.assertEquals(expectedStr,
                node.toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL()));
    }
    @Test
    public void testNodeEL4(){
        Map<String, Object> name2Value = new HashMap<>();
        name2Value.put("name", "zhangsan");
        name2Value.put("age", 18);
        String expectedStr = "nodeData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        NodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
        Assertions.assertEquals(expectedStr,
                node.toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL(true)));
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
    public void testNodeEL5(){
        ParamClass name2Value = new ParamClass();
        name2Value.age = 18;
        name2Value.name = "zhangsan";
        String expectedStr = "nodeData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        NodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
        Assertions.assertEquals(expectedStr,
                node.toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL()));
    }
    @Test
    public void testNodeEL6(){
        ParamClass name2Value = new ParamClass();
        name2Value.age = 18;
        name2Value.name = "zhangsan";
        String expectedStr = "nodeData = '{\"name\":\"zhangsan\",\"age\":18}';\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        NodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
        Assertions.assertEquals(expectedStr,
                node.toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL(true)));
    }
}
