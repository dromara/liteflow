package com.yomahub.liteflow.test.exception;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.exception.FlowExecutorNotInitException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
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
@ContextConfiguration("classpath:/exception/application1.xml")
public class Exception1SpringTest extends BaseTest {
    
    @Resource
    private FlowExecutor flowExecutor;

    /**
     * 验证 chain 节点重复的异常
     */
    @Test(expected = FlowExecutorNotInitException.class)
    public void testChainDuplicateException() {
        try {
            LiteflowConfig config = LiteflowConfigGetter.get();
            config.setRuleSource("exception/flow-exception.xml");
            flowExecutor.init();
        } catch (Exception ex) {
            // 这里需要 catch 是因为，异常是往上抛的，最后被包装成了 FlowExecutorNotInitException
            Assert.assertTrue(ex.getMessage().contains("[chain name duplicate]"));
            throw ex;
        }
    }

    @Test(expected = ConfigErrorException.class)
    public void testConfigErrorException() {
        flowExecutor.setLiteflowConfig(null);
        flowExecutor.init();
    }

    @Test(expected = FlowExecutorNotInitException.class)
    public void testFlowExecutorNotInitException() {
        LiteflowConfig config = LiteflowConfigGetter.get();
        config.setRuleSource("error/flow.txt");
        flowExecutor.init();
    }
}
