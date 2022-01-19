package com.yomahub.liteflow.test.monitor;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
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
 * springboot环境最普通的例子测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/monitor/application.properties")
@SpringBootTest(classes = MonitorSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.monitor.cmp"})
public class MonitorSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testBase() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());

        Thread.sleep(20000);
    }

}
