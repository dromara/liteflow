package com.yomahub.liteflow.test.whenTimeOut;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.WhenTimeoutException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * springboot环境下异步线程超时日志打印测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/whenTimeOut/application1.properties")
public class WhenTimeOutELSpringbootTest1 extends BaseTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Inject
    private FlowExecutor flowExecutor;

    //其中b和c在when情况下超时，所以抛出了WhenTimeoutException这个错
    @Test
    public void testWhenTimeOut() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(WhenTimeoutException.class, response.getCause().getClass());
    }
}
