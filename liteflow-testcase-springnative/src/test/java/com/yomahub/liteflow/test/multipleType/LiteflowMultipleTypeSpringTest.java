package com.yomahub.liteflow.test.multipleType;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultSlot;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;


/**
 * 测试spring下混合格式规则的场景
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/multipleType/application.xml")
public class LiteflowMultipleTypeSpringTest extends BaseTest {

    @Autowired
    private FlowExecutor flowExecutor;

    @Test
    public void testMultipleType() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>b==>a", response.getSlot().getExecuteStepStr());
        response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c", response.getSlot().getExecuteStepStr());
    }
}
