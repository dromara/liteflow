package com.yomahub.liteflow.test.subflow2;

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
 * 测试显示调用子流程(json)
 * 单元测试
 *
 * @author justin.xu
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/subflow/application-json.properties")
public class SubflowJsonELSpringBootTest extends BaseTest {
    @Inject
    private FlowExecutor flowExecutor;

    //是否按照流程定义配置执行
    @Test
    public void testExplicitSubFlow() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getExecuteStepStr());
    }
}
