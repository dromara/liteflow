package com.yomahub.liteflow.test.customWhenThreadPool;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下异步线程超时日志打印测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/customWhenThreadPool/application.properties")
@SpringBootTest(classes = CustomWhenThreadPoolSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.customWhenThreadPool.cmp"})
public class CustomWhenThreadPoolSpringbootTest extends BaseTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testCustomThreadPool() throws Exception{
        LiteflowResponse<DefaultSlot> response1 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response1.isSuccess());
        Assert.assertTrue(response1.getSlot().getData("threadName").toString().startsWith("lf-when-thead"));

        LiteflowResponse<DefaultSlot> response2 = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response2.isSuccess());
        Assert.assertTrue(response2.getSlot().getData("threadName").toString().startsWith("Customer-when-1-thead"));

        LiteflowResponse<DefaultSlot> response3 = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response3.isSuccess());
        Assert.assertTrue(response3.getSlot().getData("threadName").toString().startsWith("Customer-when-2-thead"));
    }
}
