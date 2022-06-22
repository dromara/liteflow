package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.LiteFlowConditionBuilder;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.builder.entity.ExecutableEntity;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.builder.cmp1.ACmp;
import com.yomahub.liteflow.test.builder.cmp1.BCmp;
import com.yomahub.liteflow.test.builder.cmp1.CCmp;
import com.yomahub.liteflow.test.builder.cmp1.DCmp;
import com.yomahub.liteflow.test.builder.cmp1.ECmp;
import com.yomahub.liteflow.test.builder.cmp1.FCmp;
import com.yomahub.liteflow.test.builder.cmp1.GCmp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

//基于builder模式的单元测试
//这里只是最基本的builder模式的测试，只是为了验证在springboot模式下的正常性
//更详细的builder模式测试用例会单独拉testcase去做
@RunWith(SpringRunner.class)
@SpringBootTest(classes = BuilderSpringbootTest1.class)
@EnableAutoConfiguration
public class BuilderSpringbootTest1 extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //基于普通组件的builder模式测试
    @Test
    public void testBuilder() throws Exception {
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.ACmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.BCmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.CCmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("d")
                .setName("组件D")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.DCmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("e")
                .setName("组件E")
                .setType(NodeTypeEnum.SWITCH)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.ECmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("f")
                .setName("组件F")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.FCmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("g")
                .setName("组件G")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp1.GCmp")
                .build();


        LiteFlowChainBuilder.createChain().setChainName("chain2").setCondition(
                LiteFlowConditionBuilder.createThenCondition().setValue("c,d").build()
        ).build();

        LiteFlowChainBuilder.createChain().setChainName("chain1").setCondition(
                LiteFlowConditionBuilder.createThenCondition().setValue("a,b").build()
        ).setCondition(
                LiteFlowConditionBuilder.createWhenCondition().setValue("e(f|g|chain2)").build()
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a[组件A]==>b[组件B]==>e[组件E]==>c[组件C]==>d[组件D]", response.getExecuteStepStr());
    }

    //基于普通组件的builder模式测试
    @Test
    public void testBuilderForClassAndCode() throws Exception {
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(BCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(CCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("d")
                .setName("组件D")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(DCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("e")
                .setName("组件E")
                .setType(NodeTypeEnum.SWITCH)
                .setNodeComponentClazz(ECmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("f")
                .setName("组件F")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(FCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("g")
                .setName("组件G")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(GCmp.class)
                .build();


        LiteFlowChainBuilder.createChain().setChainName("chain2").setCondition(
                LiteFlowConditionBuilder.createThenCondition().setValue("c,d").build()
        ).build();

        LiteFlowChainBuilder.createChain().setChainName("chain1").setCondition(
                LiteFlowConditionBuilder
                        .createThenCondition()
                        .setValue("a,b").build()
        ).setCondition(
                LiteFlowConditionBuilder.createWhenCondition()
                        .setValue("e(f|g|chain2)").build()
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a[组件A]==>b[组件B]==>e[组件E]==>c[组件C]==>d[组件D]", response.getExecuteStepStr());
    }


    //基于普通组件的builder模式测试
    @Test
    public void testBuilderForConditionNode() throws Exception {
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(BCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(CCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("d")
                .setName("组件D")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(DCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("e")
                .setName("组件E")
                .setType(NodeTypeEnum.SWITCH)
                .setNodeComponentClazz(ECmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("f")
                .setName("组件F")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(FCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("g")
                .setName("组件G")
                .setType(NodeTypeEnum.COMMON)
                .setNodeComponentClazz(GCmp.class)
                .build();


        LiteFlowChainBuilder.createChain().setChainName("chain2").setCondition(
                LiteFlowConditionBuilder.createThenCondition()
                        .setExecutable(new ExecutableEntity().setId("c"))
                        .setExecutable(new ExecutableEntity().setId("d"))
                        .build()
        ).build();

        LiteFlowChainBuilder.createChain().setChainName("chain1").setCondition(
                LiteFlowConditionBuilder
                        .createThenCondition()
                        .setExecutable(new ExecutableEntity().setId("a").setTag("hello"))
                        .setExecutable(new ExecutableEntity().setId("b"))
                        .build()
        ).setCondition(
                LiteFlowConditionBuilder.createWhenCondition()
                        .setExecutable(
                                new ExecutableEntity().setId("e")
                                        .addNodeCondComponent(new ExecutableEntity().setId("f").setTag("FHello"))
                                        .addNodeCondComponent(new ExecutableEntity().setId("g"))
                                        .addNodeCondComponent(new ExecutableEntity().setId("chain2")
                                        )).build()
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a[组件A]==>b[组件B]==>e[组件E]==>c[组件C]==>d[组件D]", response.getExecuteStepStr());
    }
}
