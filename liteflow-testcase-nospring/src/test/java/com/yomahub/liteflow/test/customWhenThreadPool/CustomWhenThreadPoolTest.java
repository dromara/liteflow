package com.yomahub.liteflow.test.customWhenThreadPool;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * nospring环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
public class CustomWhenThreadPoolTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeClass
    public static void init(){
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("customWhenThreadPool/flow.xml");
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    /**
     * 测试全局线程池配置
     */
    @Test
    public void testGlobalThreadPool() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertTrue(response.getSlot().getData("threadName").toString().startsWith("lf-when-thead"));
    }

    /**
     * 测试全局和when上自定义线程池-优先以when上为准
     */
    @Test
    public void testGlobalAndCustomWhenThreadPool() {
        LiteflowResponse<DefaultSlot> response1 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response1.isSuccess());
        Assert.assertTrue(response1.getSlot().getData("threadName").toString().startsWith("customer-when-1-thead"));
    }


    /**
     * when配置的线程池可以共用
     */
    @Test
    public void testCustomWhenThreadPool() {
        // 使用when - thread1
        testGlobalAndCustomWhenThreadPool();
        // chain配置同一个thead1
        LiteflowResponse<DefaultSlot> response2 = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response2.isSuccess());
        Assert.assertTrue(response2.getSlot().getData("threadName").toString().startsWith("customer-when-1-thead"));

    }
}
