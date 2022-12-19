package com.yomahub.liteflow.test.validateRule;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.base.BaseELSpringbootTest;
import com.yomahub.liteflow.test.validateRule.cmp.ACmp;
import com.yomahub.liteflow.test.validateRule.cmp.BCmp;
import com.yomahub.liteflow.test.validateRule.cmp.CCmp;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ValidateRuleELSpringbootTest.class)
@EnableAutoConfiguration
public class ValidateRuleELSpringbootTest extends BaseTest {

    @Test
    public void testChainELExpressValidate() {
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
        Assert.assertFalse(LiteFlowChainELBuilder.validate("THEN(a, b, h)"));
        Assert.assertTrue(LiteFlowChainELBuilder.validate("THEN(a, b, c)"));
    }
}
