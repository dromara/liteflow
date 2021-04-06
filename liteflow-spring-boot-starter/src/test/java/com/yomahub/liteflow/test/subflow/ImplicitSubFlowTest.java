package com.yomahub.liteflow.test.subflow;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
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
@SpringBootTest(classes = ImplicitSubFlowTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.subflow.cmp2"})
public class ImplicitSubFlowTest {
    @Resource
    private FlowExecutor flowExecutor;

    public static final Set<Integer> RUN_TIME_SLOT = new HashSet<>();

    //这里GCmp中隐式的调用chain4，从而执行了h，m
    @Test
    public void testImplicitSubFlow() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain3", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("f==>g==>h==>m", response.getData().printStep());

        //  传递了slotIndex，则set的size==1
        Assert.assertEquals(1, RUN_TIME_SLOT.size());
    }
}
