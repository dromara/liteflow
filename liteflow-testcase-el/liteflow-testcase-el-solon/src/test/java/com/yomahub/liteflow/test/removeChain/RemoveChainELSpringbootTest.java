package com.yomahub.liteflow.test.removeChain;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境最普通的例子测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/removeChain/application.properties")
public class RemoveChainELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testRemoveChain() throws Exception{
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response1.isSuccess());
        FlowBus.removeChain("chain1");
        LiteflowResponse response2 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertFalse(response2.isSuccess());
    }

}
