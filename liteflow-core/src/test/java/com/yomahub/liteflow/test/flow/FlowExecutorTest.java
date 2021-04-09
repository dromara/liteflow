package com.yomahub.liteflow.test.flow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.property.LiteflowConfig;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.util.ReflectionUtils;

public class FlowExecutorTest {
    
    @Test
    public void testMethodExecute() {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("flow/flow.json");
        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<CustomSlot> response = executor.execute("chain1", "test0", CustomSlot.class);
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("custom", response.getSlot().getName());
    }
    
    @Test(expected=RuntimeException.class)
    public void testMethodExecuteWithException() throws Exception{
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("flow/flow.json");
        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<CustomSlot> response = executor.execute("chain1", "test1", CustomSlot.class);
        Assert.assertFalse(response.isSuccess());
        ReflectionUtils.rethrowException(response.getCause());
    }
    
    @Test
    public void testMethodInvoke() throws Exception {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("flow/flow.json");
        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        executor.init();
        CustomSlot slot = executor.invoke("chain1", "test0", CustomSlot.class);
        Assert.assertEquals("custom", slot.getName());
    }
    
    @Test(expected=RuntimeException.class)
    public void testMethodInvokeWithException() throws Exception {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("flow/flow.json");
        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(config);
        executor.init();
        Slot slot = executor.invoke("chain1", "test1", CustomSlot.class);
    
    }
}
