package com.yomahub.liteflow.test.ruleCache;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.rollback.RollbackSpringbootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@TestPropertySource(value = "classpath:/ruleCache/application.properties")
@SpringBootTest(classes = RuleCacheSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.ruleCache.cmp" })
public class RuleCacheSpringbootTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testRuleCache() throws Exception{

    }
}
