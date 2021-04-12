package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
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

import javax.annotation.Resource;

/**
 * springboot环境下参数单元测试
 * @author zendwang
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/config/application-local.properties")
@SpringBootTest(classes = LiteflowConfigSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.config.cmp"})
public class LiteflowConfigSpringbootTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;
    
    @Autowired
    private ApplicationContext context;
    
    @Test
    public void testConfig() {
        LiteflowConfig config = context.getBean(LiteflowConfig.class);
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("config/flow.yml", config.getRuleSource());
        Assert.assertEquals(15, config.getWhenMaxWaitSeconds().intValue());
        Assert.assertEquals(200, config.getQueueLimit().intValue());
        Assert.assertEquals(300000L, config.getDelay().longValue());
        Assert.assertEquals(300000L, config.getPeriod().longValue());
        Assert.assertFalse(config.getEnableLog());
        Assert.assertEquals(4, config.getWhenMaxWorkers().longValue());
        Assert.assertEquals(512, config.getWhenQueueLimit().longValue());
    }
}
