package com.yomahub.liteflow.test.nullParam;

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
 * 单元测试:传递null param导致NPE的优化代码
 *
 * @author LeoLee
 * @since 2.6.6
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/nullParam/application.properties")
public class NullParamELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    /**
     * 支持无参的flow执行，以及param 为null时的异常抛出
     */
    @Test
    public void testNullParam() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1");
        Assert.assertTrue(response.isSuccess());
    }

}
