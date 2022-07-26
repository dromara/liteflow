package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.LiteFlowException;
import com.yomahub.liteflow.exception.NoSwitchTargetNodeException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 流程执行异常
 * 单元测试
 *
 * @author zendwang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/exception/application2.xml")
public class Exception2ELSpringTest extends BaseTest {
    
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
    
    @Test
    public void testGetSlotFromResponseWhenException() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "test");
        Assert.assertFalse(response.isSuccess());
        Assert.assertNotNull(response.getCause());
        Assert.assertNotNull(response.getSlot());
    }

    @Test(expected = NoSwitchTargetNodeException.class)
    public void testNoTargetFindException() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "test");
        Assert.assertFalse(response.isSuccess());
        throw response.getCause();
    }

    @Test
    public void testInvokeCustomStatefulException() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", "custom-stateful-exception");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("300", response.getCode());
        Assert.assertNotNull(response.getCause());
        Assert.assertTrue(response.getCause() instanceof LiteFlowException);
        Assert.assertNotNull(response.getSlot());
    }

    @Test
    public void testNotInvokeCustomStatefulException() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", "test");
        Assert.assertTrue(response.isSuccess());
        Assert.assertNull(response.getCode());
    }
}
