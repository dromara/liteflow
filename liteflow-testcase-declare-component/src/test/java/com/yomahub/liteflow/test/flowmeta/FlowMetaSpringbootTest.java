package com.yomahub.liteflow.test.flowmeta;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.flowmeta.cmp2.DCmp;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/flowmeta/application.properties")
@SpringBootTest(classes = FlowMetaSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.flowmeta.cmp1"})
public class FlowMetaSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试动态添加元信息节点
    @Test
    public void testFlowMeta() {
        FlowBus.addCommonNode("d", "d组件", DCmp.class.getName());
        LiteflowResponse<DefaultContext> response= flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>b==>c==>d[d组件]", response.getSlot().getExecuteStepStr());
    }
}
