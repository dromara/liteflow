package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.exception.ChainDuplicateException;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.ELParseException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 流程执行异常
 * 单元测试
 *
 * @author zendwang
 */
public class Exception1Test extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("exception/flow.el.xml");
        config.setWhenMaxWaitSeconds(1);
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    /**
     * 验证 chain 节点重复的异常
     */
    @Test(expected = ChainDuplicateException.class)
    public void testChainDuplicateException() {
        LiteflowConfig config = LiteflowConfigGetter.get();
        config.setRuleSource("exception/flow-exception.el.xml");
        flowExecutor.reloadRule();
    }

    @Test(expected = ConfigErrorException.class)
    public void testConfigErrorException() {
        flowExecutor.setLiteflowConfig(null);
        flowExecutor.reloadRule();
    }

    @Test(expected = FlowExecutorNotInitException.class)
    public void testFlowExecutorNotInitException() {
        LiteflowConfig config = LiteflowConfigGetter.get();
        config.setRuleSource("error/flow.txt");
        flowExecutor.reloadRule();
    }

    @Test
    public void testChainElBuilderOnlyValidate() {
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp.ACmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp.BCmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp.CCmp")
                .build();
        try {
            LiteFlowChainELBuilder.createChain().setChainId("chain3").setEL(
                    "THEN(a, b, d)"
            ).setOnlyValidate().build();
        } catch ( Exception ex) {
            Assert.assertTrue(ex instanceof ELParseException);
        }
    }
}
