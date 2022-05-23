package com.yomahub.liteflow.test.useTTLInWhen;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultSlot;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 在when异步节点的情况下去拿ThreadLocal里的测试场景
 * @author Bryan.Zhang
 * @since 2.6.3
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/useTTLInWhen/application.xml")
public class UseTTLInWhenSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testUseTTLInWhen() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertEquals("hello,b", response.getSlot().getData("b"));
        Assert.assertEquals("hello,c", response.getSlot().getData("c"));
        Assert.assertEquals("hello,d", response.getSlot().getData("d"));
        Assert.assertEquals("hello,e", response.getSlot().getData("e"));
        Assert.assertEquals("hello,f", response.getSlot().getData("f"));
    }
}
