package com.yomahub.liteflow.test.customThreadPool;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下异步线程超时日志打印测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/customThreadPool/application.xml")
public class CustomThreadPoolSpringTest extends BaseTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testCustomThreadPool() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertFalse(response.getSlot().getData("threadName").toString().startsWith("lf-when-thead"));
    }
}
