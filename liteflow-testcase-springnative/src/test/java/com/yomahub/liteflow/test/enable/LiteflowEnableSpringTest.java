package com.yomahub.liteflow.test.enable;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * spring环境下enable参数
 *
 * @author qjwyss
 * @since 2.6.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/enable/application-local.xml")
public class LiteflowEnableSpringTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testEnable() throws Exception {
        LiteflowConfig config = LiteflowConfigGetter.get();
        Boolean enable = config.getEnable();
        if (enable) {
            LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
            Assert.assertTrue(response.isSuccess());
            return;
        }

        Assert.assertFalse(enable);
    }
}
