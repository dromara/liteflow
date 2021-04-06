package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.exception.FlowSystemException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
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
@SpringBootTest(classes = FlowExecutorSpringBootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.exception.cmp1"})
public class FlowExecutorSpringBootTest extends BaseTest {
    
    @Resource
    private FlowExecutor flowExecutor;

    @Autowired
    private ApplicationContext context;

    @Test(expected = ConfigErrorException.class)
    public void testConfigErrorException() {
        LiteflowConfig config = context.getBean(LiteflowConfig.class);
        config.setRuleSource("");
        flowExecutor.init();
    }

    @Test(expected = FlowExecutorNotInitException.class)
    public void testFlowExecutorNotInitException() {
        LiteflowConfig config = context.getBean(LiteflowConfig.class);
        config.setRuleSource("error/flow.txt");
        flowExecutor.init();
    }

    @Test(expected = ChainNotFoundException.class)
    public void testChainNotFoundException() throws Exception {
        flowExecutor.execute("chain0", "it's a request");
    }

    @Test(expected = RuntimeException.class)
    public void testComponentCustomException() throws Exception {
        LiteflowResponse response = flowExecutor.execute("chain1", "exception");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("chain execute execption", response.getMessage());
        ReflectionUtils.rethrowException(response.getCause());
    }
    
    @Test(expected = FlowSystemException.class)
    public void testNoConditionInChainException() throws Exception {
        LiteflowResponse response = flowExecutor.execute("chain2", "test");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("no conditionList in this chain[chain2]", response.getMessage());
        ReflectionUtils.rethrowException(response.getCause());
    }
    
//    @Test(expected = WhenExecuteException.class)
//    public void testWhenExecuteTimeoutException() throws Exception {
//        LiteflowResponse response = flowExecutor.execute("chain3", "when");
//        Assert.assertFalse(response.isSuccess());
//        ReflectionUtils.rethrowException(response.getCause());
//    }
}
