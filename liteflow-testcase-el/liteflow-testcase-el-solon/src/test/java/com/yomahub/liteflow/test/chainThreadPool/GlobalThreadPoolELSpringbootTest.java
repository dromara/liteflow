package com.yomahub.liteflow.test.chainThreadPool;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.noear.solon.annotation.Import;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonTest;

import java.util.List;

/**
 * springboot环境下Global线程池隔离测试
 */
@SolonTest
@Import(profiles = "classpath:/chainThreadPool/application3.properties")
public class GlobalThreadPoolELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    /**
     * 测试WHEN上全局线程池
     */
    @Test
    public void testGlobalThreadPool() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-global-thead"));
    }

    /**
     * 测试FOR上全局线程池
     */
    @Test
    public void testGlobalThreadPool2() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-global-thead"));
    }

    /**
     * 测试WHILE上全局线程池
     */
    @Test
    public void testGlobalThreadPool3() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-global-thead"));
    }

    /**
     * 测试ITERATOR上全局线程池
     */
    @Test
    public void testGlobalThreadPool4() {
        List<String> list = ListUtil.toList("1", "2", "3", "4", "5");
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain4", list);
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-global-thead"));
    }

}
