package com.yomahub.liteflow.test.base;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class BaseCommonTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("base/flow.xml");
        flowExecutor = FlowExecutor.loadInstance(config);
    }

    @Test
    public void testBase(){
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "test0");
        Assert.assertTrue(response.isSuccess());
    }
}
