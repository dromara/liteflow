package com.yomahub.liteflow.test.parsecustom;

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

/**
 * spring环境的自定义json parser单元测试
 * @author dongguo.tao
 * @since 2.5.0
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/parsecustom/application.xml")
public class CustomParserJsonELSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试spring场景的自定义json parser
    @Test
    public void testJsonCustomParser() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
        Assert.assertTrue(response.isSuccess());
    }
}
