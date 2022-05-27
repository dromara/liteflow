package com.yomahub.liteflow.test.absoluteConfigPath;

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

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/absoluteConfigPath/application.xml")
public class AbsoluteConfigPathSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testAbsoluteConfig(){
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
