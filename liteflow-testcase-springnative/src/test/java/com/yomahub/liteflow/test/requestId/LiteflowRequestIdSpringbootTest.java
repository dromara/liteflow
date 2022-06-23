package com.yomahub.liteflow.test.requestId;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.id.IdGeneratorHelper;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * @author tangkc
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/requestId/application.xml")
@ComponentScan({"com.yomahub.liteflow.test.requestId.cmp"})
public class LiteflowRequestIdSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testRequestId() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(response.getSlot().getRequestId(), "1");
    }

}
