package com.yomahub.liteflow.test.customWhenThreadPool;

import com.yomahub.liteflow.builder.LiteFlowChainBuilder;
import com.yomahub.liteflow.builder.LiteFlowConditionBuilder;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下异步线程超时日志打印测试
 *
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/customWhenThreadPool/application.properties")
@SpringBootTest(classes = CustomWhenThreadPoolSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.customWhenThreadPool.cmp"})
public class CustomWhenThreadPoolSpringbootTest extends BaseTest {

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testCustomThreadPool() throws Exception {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertTrue(response.getSlot().getData("threadName").toString().startsWith("lf-when-thead"));

        LiteflowResponse<DefaultSlot> response1 = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response1.isSuccess());
        Assert.assertTrue(response1.getSlot().getData("threadName").toString().startsWith("customer-when-1-thead"));

        LiteflowResponse<DefaultSlot> response2 = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response2.isSuccess());
        Assert.assertTrue(response2.getSlot().getData("threadName").toString().startsWith("customer-when-2-thead"));


        // 使用build模式构建chain测试when条件的多线程
        LiteFlowNodeBuilder.createNode().setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp.ACmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp.BCmp")
                .build();
        LiteFlowNodeBuilder.createNode().setId("c")
                .setName("组件C")
                .setType(NodeTypeEnum.COMMON)
                .setClazz("com.yomahub.liteflow.test.builder.cmp.CCmp")
                .build();


        LiteFlowChainBuilder.createChain().setChainName("chain3").setCondition(
                LiteFlowConditionBuilder
                        .createWhenCondition()
                        .setThreadExecutorClass(CustomThreadExecutor3.class.getName())
                        .setValue("a,b,c,d")
                        .build()
        ).build();
        LiteflowResponse<DefaultSlot> response3 = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertTrue(response3.isSuccess());
        Assert.assertTrue(response3.getSlot().getData("threadName").toString().startsWith("customer-when-3-thead"));
    }
}
