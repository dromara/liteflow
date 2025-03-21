package com.yomahub.liteflow.test.builder;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.CommonNodeELWrapper;
import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.el.ThenELWrapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.builder.cmp.ACmp;
import com.yomahub.liteflow.test.builder.cmp.BCmp;
import com.yomahub.liteflow.test.builder.vo.User;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
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
    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testNodeEL1(){
        String jsonStr = "{\"name\":\"zhangsan\",\"age\":18}";
        String expectedStr = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        CommonNodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", jsonStr);
        Assertions.assertEquals(expectedStr,
                node.toEL());
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL()));
    }
    @Test
    public void testNodeEL2(){
        String jsonStr = "{\"name\":\"zhangsan\",\"age\":18}";
        String expectedStr = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        CommonNodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", jsonStr);
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
        String expectedStr = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        CommonNodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
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
        String expectedStr = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        CommonNodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
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
        String expectedStr = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        CommonNodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
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
        String expectedStr = "nodeData = \"{\\\"name\\\":\\\"zhangsan\\\",\\\"age\\\":18}\";\n" +
                "node(\"a\").tag(\"node a tag\").data(nodeData).maxWaitSeconds(4);";
        CommonNodeELWrapper node = ELBus.node("a").maxWaitSeconds(4).tag("node a tag").data("nodeData", name2Value);
        Assertions.assertEquals(expectedStr,
                node.toEL(true));
        System.out.println(expectedStr);
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(node.toEL(true)));
    }
    @Test
    public void testNodeData(){
        LiteFlowNodeBuilder.createNode()
                .setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode()
                .setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(BCmp.class)
                .build();

        String expectedStr = "sql = \"select * from member t where t.id=10001\";\n" +
                "jsonstr = \"{\\\"name\\\":\\\"jack\\\",\\\"age\\\":27,\\\"birth\\\":\\\"1995-10-01\\\"}\";\n" +
                "THEN(\n" +
                "\tnode(\"a\").data(sql),\n" +
                "\tnode(\"b\").data(jsonstr)\n" +
                ");";
        String param1 = "select * from member t where t.id=10001";
        String param2 = "{\"name\":\"jack\",\"age\":27,\"birth\":\"1995-10-01\"}";
        ThenELWrapper el = ELBus.then(ELBus.node("a").data("sql", param1),
                ELBus.node("b").data("jsonstr", param2));
        Assertions.assertEquals(expectedStr, el.toEL(true));

        LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL(
                el.toEL()
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        DefaultContext context = response.getFirstContextBean();
        User user = context.getData("user");
        Assertions.assertEquals(27, user.getAge());
        Assertions.assertEquals("jack", user.getName());
        Assertions.assertEquals(0, user.getBirth().compareTo(DateUtil.parseDate("1995-10-01").toJdkDate()));
    }
}
