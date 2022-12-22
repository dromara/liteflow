package com.yomahub.liteflow.test.privateDelivery;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境下隐私投递的测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/privateDelivery/application.properties")
public class PrivateDeliveryELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testPrivateDelivery() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        ConcurrentHashSet<Integer> set = context.getData("testSet");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(100, set.size());
    }
}
