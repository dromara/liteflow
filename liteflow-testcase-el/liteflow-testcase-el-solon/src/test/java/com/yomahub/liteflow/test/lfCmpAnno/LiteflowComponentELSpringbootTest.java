package com.yomahub.liteflow.test.lfCmpAnno;

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
 * 测试@LiteflowComponent标注
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/lfCmpAnno/application.properties")
public class LiteflowComponentELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testConfig() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a[A组件]==>b[B组件]==>c[C组件]==>b[B组件]==>a[A组件]==>d", response.getExecuteStepStr());
    }
}
