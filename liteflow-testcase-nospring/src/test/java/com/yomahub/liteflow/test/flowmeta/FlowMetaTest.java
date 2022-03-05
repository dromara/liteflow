package com.yomahub.liteflow.test.flowmeta;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.flowmeta.cmp2.DCmp;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FlowMetaTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("flowmeta/flow.xml");
        config.setParseOnStart(false);
        flowExecutor = FlowExecutor.loadInstance(config);
    }

    //测试动态添加元信息节点
    @Test
    public void testFlowMeta() {
        FlowBus.addCommonNode("d", "d组件", DCmp.class);
        LiteflowResponse<DefaultSlot> response= flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>d[d组件]", response.getSlot().getExecuteStepStr());
    }
}
