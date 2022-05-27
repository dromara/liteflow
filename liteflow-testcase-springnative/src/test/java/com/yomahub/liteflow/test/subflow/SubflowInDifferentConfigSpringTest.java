package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.exception.MultipleParsersException;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 测试主流程与子流程在不同的配置文件的场景
 *
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/subflow/application-subInDifferentConfig1.xml")
public class SubflowInDifferentConfigSpringTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    //是否按照流程定义配置执行
    @Test
    public void testExplicitSubFlow1() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>b==>a==>e==>d", response.getSlot().getExecuteStepStr());
    }

    //主要测试有不同的配置类型后会不会报出既定的错误
    @Test(expected = MultipleParsersException.class)
    public void testExplicitSubFlow2() {
        LiteflowConfig config = LiteflowConfigGetter.get();
        config.setRuleSource("subflow/flow-main.xml,subflow/flow-sub1.xml,subflow/flow-sub2.yml");
        flowExecutor.init();
    }
}
