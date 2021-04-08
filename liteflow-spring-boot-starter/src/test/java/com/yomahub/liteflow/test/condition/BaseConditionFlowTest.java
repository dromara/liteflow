package com.yomahub.liteflow.test.condition;

import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ReflectionUtils;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 测试隐式调用子流程
 * 单元测试
 *
 * @author ssss
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/condition/application-condition.properties")
@SpringBootTest(classes = BaseConditionFlowTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.condition.cmp1"})
public class BaseConditionFlowTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    public static final List<String> RUN_TIME_SLOT = Lists.newArrayList();

    //正常 then,when,多chain可以执行
    @Test
    public void testBaseConditionFlow() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain1", "it's a base request");
        Assert.assertTrue(response.isSuccess());
        System.out.println(response.getData().printStep());
    }

    //正常 when 多个并联 合并 errorMessage参照上一组配置 导致error异常 都可以继续执行
    @Test
    public void testBaseErrorResumeConditionFlow4() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain4", "it's a base request");
        Assert.assertTrue(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getData().getRequestId()));
    }

    //正常 when 多个并联 合并 errorMessage参照上一组配置 导致error异常 直接第一组error 就拦截
    @Test
    public void testBaseErrorResumeConditionFlow5() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain5", "it's a base request");
        System.out.println(response.isSuccess());
        System.out.println(response.getData().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getData().getRequestId()));
    }

    @Test
    public void testBaseErrorResumeConditionFlow6() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain6", "it's a base request");
        System.out.println(response.isSuccess());
        System.out.println(response.getData().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==1
        Assert.assertEquals(1, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getData().getRequestId()));
        ReflectionUtils.rethrowException(response.getCause());
    }


    @Test
    public void testBaseErrorResumeConditionFlow7() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain7", "it's a base request");
        System.out.println(response.isSuccess());
        System.out.println(response.getData().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, BaseConditionFlowTest.RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getData().getRequestId()));
    }

    @Test
    public void testBaseErrorResumeConditionFlow8() throws Exception {
        LiteflowResponse<Slot> response = flowExecutor.execute("chain8", "it's a base request");
        System.out.println(response.isSuccess());
        System.out.println(response.getData().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, BaseConditionFlowTest.RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getData().getRequestId()));
    }
}
