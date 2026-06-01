package com.yomahub.liteflow.test.agent.feature.sessionreuse;

import com.yomahub.liteflow.core.ExecuteOption;
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
 * 覆盖 guide §5.1：同一 {@code (conversationId, agentKey)} 的多次调用复用同一 ReActAgent。
 *
 * <p>通过 Hook 抓取的 agentId 判断：两次执行 agentId 一致即说明 Agent 实例被缓存复用；
 * 重置 SessionManager 后应得到不同 agentId。
 */
@TestPropertySource("classpath:/feature/sessionreuse/application.properties")
@SpringBootTest(classes = SessionReuseTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.sessionreuse")
public class SessionReuseTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        MemoryAgentCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "SessionReuseTest");
    }

    @Test
    public void testSameConversationReusesAgentInstance() {
        LiteflowResponse first = flowExecutor.execute2Resp("memoryChain", "你是谁？");
        Assertions.assertTrue(first.isSuccess(),
                "first chain failed: " + (first.getCause() == null ? "" : first.getCause().getMessage()));
        String firstAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();
        Assertions.assertNotNull(firstAgentId);

        MemoryAgentCmp.reset();
        LiteflowResponse second = flowExecutor.execute2Resp("memoryChain", "请重复一下上一句话。");
        Assertions.assertTrue(second.isSuccess(),
                "second chain failed: " + (second.getCause() == null ? "" : second.getCause().getMessage()));
        String secondAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();
        Assertions.assertEquals(firstAgentId, secondAgentId,
                "同一 (conversationId, agentKey) 多次调用应复用同一个 ReActAgent 实例");
    }

    @Test
    public void testResetSessionManagerProducesIndependentAgentInstance() {
        LiteflowResponse first = flowExecutor.execute2Resp("memoryChain", "你好");
        Assertions.assertTrue(first.isSuccess());
        String firstAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();

        // 重置 SessionManager 模拟全新 JVM 状态，确认 agentId 重新生成。
        MemoryAgentCmp.reset();
        try {
            LiveTestSupport.resetAgentSessionManager();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        LiteflowResponse second = flowExecutor.execute2Resp("memoryChain", "你好",
                ExecuteOption.of().requestId("rid-2"));
        Assertions.assertTrue(second.isSuccess());
        String secondAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();

        Assertions.assertNotEquals(firstAgentId, secondAgentId,
                "重置 SessionManager 后，新的 ReActAgent 应该获得不同的 agentId");
    }
}
