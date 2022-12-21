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

@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/parser/application-springEL.properties")
public class SpringELSupportELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试springEL的解析情况
    @Test
    public void testSpringELParser() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain11", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
