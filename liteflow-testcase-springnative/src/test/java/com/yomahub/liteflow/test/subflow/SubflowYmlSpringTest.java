package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 测试显示调用子流程(yml)
 * 单元测试
 *
 * @author justin.xu
 */
@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/subflow/application-yml.xml")
public class SubflowYmlSpringTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    //是否按照流程定义配置执行
    @Test
    public void testExplicitSubFlowYml() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>b==>a==>e==>d", response.getSlot().getExecuteStepStr());
    }
}
