package com.yomahub.liteflow.test.parsecustom;

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
 * springboot环境的自定义json parser单元测试
 * @author dongguo.tao
 * @since 2.5.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/parsecustom/application-custom-json.properties")
public class CustomParserJsonELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试springboot场景的自定义json parser
    @Test
    public void testJsonCustomParser() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "args");
        Assert.assertTrue(response.isSuccess());
    }
}
