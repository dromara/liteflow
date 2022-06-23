package com.yomahub.liteflow.test.requestId;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.id.IdGeneratorHelper;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author tangkc
 */
public class LiteflowRequestIdSpringbootTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("requestId/flow.xml");
        config.setRequestIdGeneratorClass("com.yomahub.liteflow.test.requestId.config.CustomRequestIdGenerator");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    @Test
    public void testRequestId() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(response.getSlot().getRequestId(), "1");
    }

}
