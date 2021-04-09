package com.yomahub.liteflow.test.condition;

import com.google.common.collect.Lists;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.exception.WhenExecuteException;
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
import java.util.List;

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

    public static List<String> RUN_TIME_SLOT = Lists.newArrayList();

    /*****
     * 标准chain 嵌套选择 嵌套子chain进行执行
     * 验证了when情况下 多个node是并行执行
     * 验证了默认参数情况下 when可以加载执行
     * **/
    @Test
    public void testBaseConditionFlow() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute("chain1", "it's a base request");
        Assert.assertTrue(response.isSuccess());
        System.out.println(response.getSlot().printStep());
    }

    /*****
     * 标准chain
     * 验证多层when 相同组 会合并node
     * 验证多层when errorResume 合并 并参照最上层 errorResume配置
     * **/
    @Test
    public void testBaseErrorResumeConditionFlow4() throws Exception {
        RUN_TIME_SLOT.clear();
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute("chain4", "it's a base request");
        Assert.assertTrue(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        Assert.assertTrue(RUN_TIME_SLOT.contains(response.getSlot().getRequestId()));

    }

    /*****
     * 标准chain
     * 验证多层when 相同组 会合并node
     * 验证多层when errorResume 合并 并参照最上层 errorResume配置
     * **/
    @Test(expected = WhenExecuteException.class)
    public void testBaseErrorResumeConditionFlow5() throws Exception {
        RUN_TIME_SLOT.clear();
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute("chain5", "it's a base request");
        System.out.println(response.isSuccess());
        //System.out.println(response.getSlot().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        //Assert.assertTrue(RUN_TIME_SLOT.contains(response.getSlot().getRequestId()));
        ReflectionUtils.rethrowException(response.getCause());
    }

    /*****
     * 标准chain
     * 验证多层when 不同组 不会合并node
     * 验证多层when errorResume 不同组 配置分开配置
     * **/
    @Test(expected = WhenExecuteException.class)
    public void testBaseErrorResumeConditionFlow6() throws Exception {
        RUN_TIME_SLOT.clear();
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute("chain6", "it's a base request");
        System.out.println(response.isSuccess());
        //System.out.println(response.getSlot().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==1
        Assert.assertEquals(1, RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        //Assert.assertTrue(RUN_TIME_SLOT.contains(response.getSlot().getRequestId()));
        ReflectionUtils.rethrowException(response.getCause());

    }

    /*****
     * 标准chain
     * 验证多层when 不同组 不会合并node
     * 验证多层when errorResume 不同组 配置分开配置
     * **/
    @Test(expected = WhenExecuteException.class)
    public void testBaseErrorResumeConditionFlow7() throws Exception {
        RUN_TIME_SLOT.clear();
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute("chain7", "it's a base request");
        System.out.println(response.isSuccess());
        //System.out.println(response.getSlot().printStep());
        Assert.assertFalse(response.isSuccess());
        //  传递了slotIndex，则set的size==2
        Assert.assertEquals(2, BaseConditionFlowTest.RUN_TIME_SLOT.size());
        //  set中第一次设置的requestId和response中的requestId一致
        //Assert.assertTrue(RUN_TIME_SLOT.contains(response.getSlot().getRequestId()));
        ReflectionUtils.rethrowException(response.getCause());

    }
}
