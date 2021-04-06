package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

/**
 * 无spring环境下参数单元测试
 * @author zendwang
 * @since 2.5.0
 */
public class LiteflowConfigNoSpringTest extends BaseTest {
   
    @Test(expected = ConfigErrorException.class)
    public void testNoConfig() throws Exception {
        FlowExecutor executor = new FlowExecutor();
        executor.init();
    }
    
    @Test
    public void testConfig() throws Exception {
        FlowExecutor executor = new FlowExecutor();
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("config/flow.json");
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<Slot> response = executor.execute("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(15, config.getWhenMaxWaitSeconds().intValue());
        Assert.assertEquals(200, config.getQueueLimit().intValue());
        Assert.assertEquals(300000L, config.getDelay().longValue());
        Assert.assertEquals(300000L, config.getPeriod().longValue());
        Assert.assertFalse(config.getEnableLog());
        Assert.assertEquals(Runtime.getRuntime().availableProcessors() * 2, config.getWhenMaxWorkers().longValue());
        Assert.assertEquals(512, config.getWhenQueueLimit().longValue());
    }
    
}
