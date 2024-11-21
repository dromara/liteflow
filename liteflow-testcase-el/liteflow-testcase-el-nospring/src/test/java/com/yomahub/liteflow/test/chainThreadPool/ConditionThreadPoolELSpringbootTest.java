package com.yomahub.liteflow.test.chainThreadPool;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * springboot环境下chain线程池隔离测试
 */
public class ConditionThreadPoolELSpringbootTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeAll
    public static void init() {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("chainThreadPool/flow2.el.xml");
        config.setWhenThreadPoolIsolate(true);
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    /**
     * 测试WEHN上condition线程池和chain线程池隔离-优先以WHEN上为准
     */
    @Test
    public void testConditionThreadPool() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-when-thead"));
    }

    /**
     * 测试FOR上condition线程池和chain线程池隔离-优先以FOR上为准
     */
    @Test
    public void testConditionThreadPool2() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-loop-thead"));
    }

    /**
     * 测试WHILE上condition线程池和chain线程池隔离-优先以WHILE上为准
     */
    @Test
    public void testConditionThreadPool3() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-loop-thead"));
    }

    /**
     * 测试ITERATOR上condition线程池和chain线程池隔离-优先以ITERATOR上为准
     */
    @Test
    public void testConditionThreadPool4() {
        List<String> list = ListUtil.toList("1", "2", "3", "4", "5");
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain4", list);
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-loop-thead"));
    }

}
