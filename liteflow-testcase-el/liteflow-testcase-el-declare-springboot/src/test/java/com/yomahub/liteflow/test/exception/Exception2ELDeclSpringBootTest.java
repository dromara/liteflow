package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.*;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
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
@TestPropertySource(value = "classpath:/exception/application.properties")
@SpringBootTest(classes = Exception2ELDeclSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.exception.cmp"})
public class Exception2ELDeclSpringBootTest extends BaseTest {
    
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
}
