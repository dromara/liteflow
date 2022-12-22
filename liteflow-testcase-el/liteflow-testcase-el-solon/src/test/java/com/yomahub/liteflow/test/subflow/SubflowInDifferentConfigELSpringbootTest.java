package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.MultipleParsersException;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.core.AopContext;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * 测试主流程与子流程在不同的配置文件的场景
 *
 * @author Bryan.Zhang
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/subflow/application-subInDifferentConfig1.properties")
public class SubflowInDifferentConfigELSpringbootTest extends BaseTest {
    @Inject
    private FlowExecutor flowExecutor;

    //是否按照流程定义配置执行
    @Test
    public void testExplicitSubFlow1() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>b==>a==>e==>d", response.getExecuteStepStr());
    }

    @Inject
    private AopContext context;

    //主要测试有不同的配置类型后会不会报出既定的错误
    @Test(expected = MultipleParsersException.class)
    public void testExplicitSubFlow2() {
        LiteflowConfig config = context.getBean(LiteflowConfig.class);
        config.setRuleSource("subflow/flow-main.el.xml,subflow/flow-sub1.el.xml,subflow/flow-sub2.el.yml");
        flowExecutor.reloadRule();
    }
}
