package com.yomahub.liteflow.test.executor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

/**
 * 无spring环境下FlowExecutor
 * 调用方法execute、invoke单元测试
 * @author zendwang
 * @since 2.5.1
 */
public class FlowExecutorTest extends BaseTest {
    
    @Test
    public void testMethodExecute2Resp() {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("executor/flow.json");
        FlowExecutor executor = new FlowExecutor(config);
        LiteflowResponse<CustomSlot> response = executor.execute2Resp("chain1", "test0", CustomSlot.class);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("custom", response.getSlot().getName());
    }
    
    @Test(expected=RuntimeException.class)
    public void testMethodExecute2RespWithException() throws Exception{
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("executor/flow0.json");
        FlowExecutor executor = new FlowExecutor(config);
        LiteflowResponse<CustomSlot> response = executor.execute2Resp("chain1", "test1", CustomSlot.class);
        Assert.assertFalse(response.isSuccess());
        ReflectionUtils.rethrowException(response.getCause());
    }
    
    @Test
    public void testMethodExecute() throws Exception {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("executor/flow.json");
        FlowExecutor executor = new FlowExecutor(config);
        CustomSlot slot = executor.execute("chain1", "test0", CustomSlot.class);
        Assert.assertEquals("custom", slot.getName());
    }
    
    @Test(expected=RuntimeException.class)
    public void testMethodExecuteWithException() throws Exception {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("executor/flow0.json");
        FlowExecutor executor = new FlowExecutor(config);
        executor.execute("chain1", "test1", CustomSlot.class);
    }
}
