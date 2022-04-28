package com.yomahub.liteflow.test.removeChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class RemoveChainTest extends BaseTest{

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("removeChain/flow.xml");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    @Test
    public void testRemoveChain(){
        LiteflowResponse<DefaultSlot> response1 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response1.isSuccess());
        FlowBus.removeChain("chain1");
        LiteflowResponse<DefaultSlot> response2 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertFalse(response2.isSuccess());
    }
}
