package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;

/**
 * 流程执行异常
 * 单元测试
 *
 * @author zendwang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/exception/application2.xml")
public class Exception2SpringTest extends BaseTest {
    
    @Resource
    private FlowExecutor flowExecutor;

    @Test(expected = ChainNotFoundException.class)
    public void testChainNotFoundException() throws Exception {
        flowExecutor.execute("chain0", "it's a request");
    }

    @Test(expected = RuntimeException.class)
    public void testComponentCustomException() throws Exception {
        flowExecutor.execute("chain1", "exception");
    }

    @Test(expected = FlowSystemException.class)
    public void testNoConditionInChainException() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "test");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("no conditionList in this chain[chain2]", response.getMessage());
        ReflectionUtils.rethrowException(response.getCause());
    }

    @Test
    public void testGetSlotFromResponseWhenException() throws Exception{
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain4", "test");
        Assert.assertFalse(response.isSuccess());
        Assert.assertNotNull(response.getCause());
        Assert.assertNotNull(response.getSlot());
    }
}
