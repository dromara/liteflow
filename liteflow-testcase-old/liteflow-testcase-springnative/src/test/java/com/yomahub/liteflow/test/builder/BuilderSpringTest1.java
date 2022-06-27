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
import com.yomahub.liteflow.test.builder.cmp1.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

//基于builder模式的单元测试
//这里只是最基本的builder模式的测试，只是为了验证在spring模式下的正常性
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/builder/application1.xml")
public class BuilderSpringTest1 extends BaseTest {

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
                LiteFlowConditionBuilder
                        .createWhenCondition()
                        .setValue("a,b").build()
        ).setCondition(
                LiteFlowConditionBuilder.createWhenCondition()
                        .setValue("e(f|g|chain2)").build()
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
    }


    //基于普通组件的builder模式测试
    @Test
    public void testBuilderForClassAndCode() throws Exception {
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(BCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(CCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("d")
                .setName("组件D")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(DCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("e")
                .setName("组件E")
                .setType(NodeTypeEnum.SWITCH)
                .setClazz(ECmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("f")
                .setName("组件F")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(FCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("g")
                .setName("组件G")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(GCmp.class)
                .build();


        LiteFlowChainBuilder.createChain().setChainName("chain2").setCondition(
                LiteFlowConditionBuilder.createThenCondition().setValue("c,d").build()
        ).build();

        LiteFlowChainBuilder.createChain().setChainName("chain1").setCondition(
                LiteFlowConditionBuilder
                        .createWhenCondition()
                        .setValue("a[hello],b").build()
        ).setCondition(
                LiteFlowConditionBuilder.createWhenCondition()
                        .setValue("e(f|g|chain2)").build()
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
    }

    //基于普通组件的builder模式测试
    @Test
    public void testBuilderForConditionNode() throws Exception {
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(BCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(CCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("d")
                .setName("组件D")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(DCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("e")
                .setName("组件E")
                .setType(NodeTypeEnum.SWITCH)
                .setClazz(ECmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("f")
                .setName("组件F")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(FCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("g")
                .setName("组件G")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(GCmp.class)
                .build();


        LiteFlowChainBuilder.createChain().setChainName("chain2").setCondition(
                LiteFlowConditionBuilder.createThenCondition()
                        .setExecutable(new ExecutableEntity().setId("c"))
                        .setExecutable(new ExecutableEntity().setId("d"))
                        .build()
        ).build();

        LiteFlowChainBuilder.createChain().setChainName("chain1").setCondition(
                LiteFlowConditionBuilder
                        .createWhenCondition()
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
    }
}
