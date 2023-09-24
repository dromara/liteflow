package com.yomahub.liteflow.test.fallback;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.test.execute2Future.Executor2FutureELSpringbootTest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;

@TestPropertySource(value = "classpath:/fallback/application.properties")
@SpringBootTest(classes = FallbackSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.fallback.cmp" })
public class FallbackSpringbootTest {
    @Resource
    private FlowExecutor flowExecutor;
    
    @Test
    public void test1() {
        flowExecutor.execute2Resp("chain1");
    }
    
    @Test
    public void test2() {
        flowExecutor.execute2Resp("chain2");
    }
}
