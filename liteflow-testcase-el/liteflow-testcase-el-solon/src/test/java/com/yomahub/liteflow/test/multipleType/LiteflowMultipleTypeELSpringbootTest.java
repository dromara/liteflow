package com.yomahub.liteflow.test.multipleType;

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
 * 测试springboot下混合格式规则的场景
 * @author Bryan.Zhang
 * @since 2.5.10
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/multipleType/application.properties")
public class LiteflowMultipleTypeELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testMultipleType() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>b==>a", response.getExecuteStepStr());
        response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }
}
