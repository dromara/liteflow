package com.yomahub.liteflow.test.requestId;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * @author tangkc
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/requestId/application.properties")
public class LiteflowRequestIdELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testRequestId() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("1", response.getRequestId());
    }

}
