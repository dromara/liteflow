package com.yomahub.liteflow.test.execute2Future;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.Future;

/**
 * spring环境执行返回future的例子
 * @author Bryan.Zhang
 * @since 2.6.13
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/execute2Future/application.xml")
public class Executor2FutureSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testFuture() throws Exception{
        Future<LiteflowResponse> future = flowExecutor.execute2Future("chain1", "arg", DefaultContext.class);
        LiteflowResponse response = future.get();
        Assert.assertTrue(response.isSuccess());
    }

}
