package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ConfigErrorException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
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
    
    @Test(expected = ConfigErrorException.class)
    public void testConfigError() throws Exception {
        LiteflowConfig liteflowConfig = new LiteflowConfig();
        liteflowConfig.setRuleSource("json:127.0.0.1:2181:/");
        
        FlowExecutor executor = new FlowExecutor();
        executor.setLiteflowConfig(liteflowConfig);
        executor.init();
    }
    
}
