package com.yomahub.liteflow.test.chainThreadPool;

import cn.hutool.core.collection.ListUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;

/**
 * springboot环境下chain线程池隔离测试
 */
@TestPropertySource(value = "classpath:/chainThreadPool/application.properties")
@SpringBootTest(classes = ChainThreadPoolELSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.chainThreadPool.cmp"})
public class ChainThreadPoolELSpringbootTest extends BaseTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private FlowExecutor flowExecutor;

    /**
     * 测试WHEN上全局线程池和chain线程池隔离-优先以chain上为准
     */
    @Test
    public void testChainThreadPool() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-chain-thead"));
    }

    /**
     * 测试FOR上全局线程池和chain线程池隔离-优先以chain上为准
     */
    @Test
    public void testChainThreadPool2() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-chain-thead"));
    }

    /**
     * 测试WHILE上全局线程池和chain线程池隔离-优先以chain上为准
     */
    @Test
    public void testChainThreadPool3() {
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-chain-thead"));
    }

    /**
     * 测试ITERATOR上全局线程池和chain线程池隔离-优先以chain上为准
     */
    @Test
    public void testChainThreadPool4() {
        List<String> list = ListUtil.toList("1", "2", "3", "4", "5");
        LiteflowResponse response1 = flowExecutor.execute2Resp("chain4", list);
        DefaultContext context = response1.getFirstContextBean();
        Assertions.assertTrue(response1.isSuccess());
        Assertions.assertTrue(context.getData("threadName").toString().startsWith("customer-chain-thead"));
    }

}
