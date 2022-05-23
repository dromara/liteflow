package com.yomahub.liteflow.test.removeChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultSlot;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/removeChain/application.xml")
public class RemoveChainSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testRemoveChain(){
        LiteflowResponse<DefaultSlot> response1 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response1.isSuccess());
        FlowBus.removeChain("chain1");
        LiteflowResponse<DefaultSlot> response2 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertFalse(response2.isSuccess());
    }
}
