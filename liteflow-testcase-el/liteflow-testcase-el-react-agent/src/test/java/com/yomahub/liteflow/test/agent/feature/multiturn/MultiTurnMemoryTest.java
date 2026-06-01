package com.yomahub.liteflow.test.agent.feature.multiturn;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.support.BaseAgentLiveTest;
import com.yomahub.liteflow.test.agent.support.LiveTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

/**
 * 覆盖 guide §8.4 多轮对话：同一 conversationId 下连续两次调用同一个 Agent，
 * 第二次复用同一个 ReActAgent 实例（来自 Hook 抓取的 agentId），两轮回复都非空，
 * 证明 memory 在 JVM 模式下能跨调用复用。
 */
@TestPropertySource("classpath:/feature/multiturn/application.properties")
@SpringBootTest(classes = MultiTurnMemoryTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.multiturn")
public class MultiTurnMemoryTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        MemoryAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "MultiTurnMemoryTest");
    }

    @Test
    public void testMultiTurnMemoryReusesAgentAcrossCalls() {
        LiteflowResponse first = flowExecutor.execute2Resp(
                "memoryChain", "请记住我喜欢的数字是 7。");
        Assertions.assertTrue(first.isSuccess(),
                "first chain failed: " + (first.getCause() == null ? "" : first.getCause().getMessage()));
        Object firstReply = first.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(firstReply);

        String firstAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();
        Assertions.assertNotNull(firstAgentId);

        MemoryAgentCmp.reset();

        LiteflowResponse second = flowExecutor.execute2Resp(
                "memoryChain", "我刚才说我喜欢的数字是多少？");
        Assertions.assertTrue(second.isSuccess(),
                "second chain failed: " + (second.getCause() == null ? "" : second.getCause().getMessage()));
        Object secondReply = second.getSlot().getOutput(RecordReplyCmp.NODE_ID);
        Assertions.assertNotNull(secondReply);

        Assertions.assertEquals(firstAgentId, MemoryAgentCmp.PROBE.get().observedAgentId(),
                "同一 (cid, agentKey) 下第二次调用应复用同一个 ReActAgent 实例（memory 已自动续接）");
    }
}
