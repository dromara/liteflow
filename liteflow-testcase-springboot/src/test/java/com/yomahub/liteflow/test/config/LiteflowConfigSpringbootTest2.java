package com.yomahub.liteflow.test.config;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
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
@TestPropertySource(value = "classpath:/config/application2.properties")
@SpringBootTest(classes = LiteflowConfigSpringbootTest2.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.config.cmp"})
public class LiteflowConfigSpringbootTest2 extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    //测试通配符
    @Test
    public void testRuleSourceMatch() {
        LiteflowResponse response0 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertEquals("a==>b==>c", response0.getExecuteStepStr());

        LiteflowResponse response1 = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertEquals("a==>c==>b==>d", response1.getExecuteStepStr());
    }
}
