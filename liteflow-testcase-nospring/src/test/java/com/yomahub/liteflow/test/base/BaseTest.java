package com.yomahub.liteflow.test.base;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.Assert;
import org.junit.Test;

public class BaseTest {

    @Test
    public void testBase(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("base/flow.xml");
        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<DefaultSlot> response = executor.execute2Resp("chain1", "test0");
        Assert.assertTrue(response.isSuccess());
    }
}
