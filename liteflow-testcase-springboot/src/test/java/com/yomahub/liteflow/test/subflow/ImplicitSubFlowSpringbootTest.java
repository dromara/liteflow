package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.Set;

/**
 * 测试隐式调用子流程
 * 单元测试
 *
 * @author justin.xu
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/subflow/application-implicit.properties")
@SpringBootTest(classes = ImplicitSubFlowSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.subflow.cmp2"})
public class ImplicitSubFlowSpringbootTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    public static final Set<String> RUN_TIME_SLOT = new HashSet<>();

    //这里GCmp中隐式的调用chain4，从而执行了h，m
    @Test
    public void testImplicitSubFlow() {
        LiteflowResponse<DefaultContext> response = flowExecutor.execute2Resp("chain3", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("f==>g==>h==>m", response.getExecuteStepStr());

        //  传递了slotIndex，则set的size==1
        Assert.assertEquals(1, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getSlot().getRequestId()));
    }
}
