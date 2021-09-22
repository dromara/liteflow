package com.yomahub.liteflow.test.cmpRetry;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;


/**
 * 测试springboot下的组件重试
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/cmpRetry/application.properties")
@SpringBootTest(classes = LiteflowRetrySpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.cmpRetry.cmp"})
public class LiteflowRetrySpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testRetry1() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>b==>b", response.getSlot().printStep());
    }

    @Test
    public void testRetry2() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("c==>c==>c==>c==>c==>c", response.getSlot().printStep());
    }

    @Test
    public void testRetry3() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertFalse(response.isSuccess());
    }

    @Test
    public void testRetry4() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("e==>e==>e==>e==>e==>e", response.getSlot().printStep());
    }
}
