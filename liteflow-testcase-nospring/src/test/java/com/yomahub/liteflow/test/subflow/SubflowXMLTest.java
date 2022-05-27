package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * 测试显示调用子流程(xml)
 * 单元测试
 *
 * @author justin.xu
 */
public class SubflowXMLTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("subflow/flow.xml");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    //是否按照流程定义配置执行
    @Test
    public void testExplicitSubFlow() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getSlot().getExecuteStepStr());
    }
}
