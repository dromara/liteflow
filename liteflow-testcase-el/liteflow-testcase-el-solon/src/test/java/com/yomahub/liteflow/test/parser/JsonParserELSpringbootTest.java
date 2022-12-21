package com.yomahub.liteflow.test.parser;

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
 * spring环境的json parser单元测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/parser/application-json.properties")
public class JsonParserELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试spring场景的json parser
    @Test
    public void testJsonParser() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
