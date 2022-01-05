package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
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
    public void testNoConfig() {
        FlowExecutor executor = new FlowExecutor();
        executor.init();
    }
    
    @Test
    public void testConfig() {
        FlowExecutor executor = new FlowExecutor();
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("config/flow.json");
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<DefaultSlot> response = executor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(15, config.getWhenMaxWaitSeconds().intValue());
        Assert.assertEquals(200, config.getQueueLimit().intValue());
        Assert.assertEquals(300000L, config.getDelay().longValue());
        Assert.assertEquals(300000L, config.getPeriod().longValue());
        Assert.assertFalse(config.getEnableLog());
        Assert.assertEquals(Runtime.getRuntime().availableProcessors() * 2, config.getWhenMaxWorkers().longValue());
        Assert.assertEquals(100, config.getWhenQueueLimit().longValue());
    }
    
    /**
     * rule source支持的通配符
     * 匹配的文件
     * config/nospringgroup0/flow0.xml
     * config/nospringgroup1/flow.xml
     */
    @Test
    public void testLocalXmlRuleSourcePatternMatch() {
        FlowExecutor executor = new FlowExecutor();
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("config/nospring*/flow*.xml");
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<DefaultSlot> response0 = executor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>b==>c", response0.getSlot().getExecuteStepStr());
        
        LiteflowResponse<DefaultSlot> response1 = executor.execute2Resp("chain3", "arg");
        Assert.assertEquals("a==>c==>f==>g", response1.getSlot().getExecuteStepStr());
        
    }
    
    /**
     * rule source支持的通配符
     * 匹配的文件
     * config/nospringgroup0/flow0.json
     * config/nospringgroup1/flow0.json
     */
    @Test
    public void testLocalJsonRuleSourcePatternMatch() {
        FlowExecutor executor = new FlowExecutor();
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("config/nospring*/flow*.json");
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<DefaultSlot> response0 = executor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>b==>c", response0.getSlot().getExecuteStepStr());
        LiteflowResponse<DefaultSlot> response1 = executor.execute2Resp("chain3", "arg");
        Assert.assertEquals("a==>c==>f==>g", response1.getSlot().getExecuteStepStr());
    }
    
    /**
     * rule source支持的通配符
     * 匹配的文件
     * config/nospringgroup0/flow0.yml
     * config/nospringgroup1/flow.yml
     */
    @Test
    public void testLocalYmlRuleSourcePatternMatch() {
        FlowExecutor executor = new FlowExecutor();
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("config/nospring*/flow*.yml");
        executor.setLiteflowConfig(config);
        executor.init();
        LiteflowResponse<DefaultSlot> response0 = executor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>b==>c", response0.getSlot().getExecuteStepStr());
        LiteflowResponse<DefaultSlot> response = executor.execute2Resp("chain3", "arg");
        Assert.assertEquals("a==>c==>f==>g", response.getSlot().getExecuteStepStr());
    }
}
