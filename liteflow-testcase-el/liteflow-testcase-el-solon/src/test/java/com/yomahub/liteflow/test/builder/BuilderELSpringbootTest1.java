package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.builder.cmp1.*;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;

//基于builder模式的单元测试
//这里只是最基本的builder模式的测试，只是为了验证在springboot模式下的正常性
//更详细的builder模式测试用例会单独拉testcase去做
@RunWith(SolonJUnit4ClassRunner.class)
public class BuilderELSpringbootTest1 extends BaseTest {

    @Inject
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


        LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL(
                "THEN(c, d)"
        ).build();

        LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL(
                "THEN(a, b, WHEN(SWITCH(e).to(f, g, chain2)))"
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


        LiteFlowChainELBuilder.createChain().setChainName("chain2").setEL(
                "THEN(c, d)"
        ).build();

        LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL(
                "THEN(a, b, WHEN(SWITCH(e).to(f, g, chain2)))"
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a[组件A]==>b[组件B]==>e[组件E]==>c[组件C]==>d[组件D]", response.getExecuteStepStr());
    }

    @Test
    public void testBuilderForSameNodeMultiTimes() throws Exception {
        LiteFlowNodeBuilder.createNode().setId("a1")
                .setName("组件A1")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("a2")
                .setName("组件A2")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("c1")
                .setName("组件C1")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(CCmp.class)
                .build();
        LiteFlowNodeBuilder.createNode().setId("c2")
                .setName("组件C2")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(CCmp.class)
                .build();

        LiteFlowChainELBuilder.createChain().setChainName("chain1").setEL(
                "THEN(a1,c2,a2,c1)"
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a1[组件A1]==>c2[组件C2]==>a2[组件A2]==>c1[组件C1]", response.getExecuteStepStr());
    }
}
