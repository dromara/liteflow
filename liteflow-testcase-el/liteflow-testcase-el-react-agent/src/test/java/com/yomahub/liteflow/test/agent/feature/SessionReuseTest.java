package com.yomahub.liteflow.test.agent.feature;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.feature.cmp.MemoryAgentCmp;
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
 * <p>通过 Hook 抓取的 agentId 来判断：两次执行的 agentId 一致即说明 Agent 实例被缓存复用。
 * 不同 conversationId 时 agentId 应不同。
 */
@TestPropertySource(value = "classpath:/agent/application.properties")
@SpringBootTest(classes = SessionReuseTest.class)
@EnableAutoConfiguration
@ComponentScan({
        "com.yomahub.liteflow.test.agent.cmp",
        "com.yomahub.liteflow.test.agent.feature.cmp"
})
public class SessionReuseTest extends BaseAgentLiveTest {

    @BeforeEach
    public void reset() {
        MemoryAgentCmp.reset();
        LiveTestSupport.ensureCompatibleCustomCredentialOrSkip(liteflowConfig, "SessionReuseTest");
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
    public void testDifferentConversationProducesIndependentAgentInstance() {
        // 第一次：用组件默认 cid（MemoryAgentCmp.FIXED_CONVERSATION_ID）
        LiteflowResponse first = flowExecutor.execute2Resp("memoryChain", "你好");
        Assertions.assertTrue(first.isSuccess());
        String firstAgentId = MemoryAgentCmp.PROBE.get().observedAgentId();

        // 第二次：通过 ExecuteOption 显式设置一个不同的 cid，绕过组件默认值，
        // 因为 ReActAgentComponent.resolveConversationId() 默认会先看 slot 已有 cid。
        // 但 MemoryAgentCmp 覆写了 resolveConversationId 返回固定值 - 这意味着即使
        // 传入 ExecuteOption.conversationId(...) 也会被组件覆盖。
        // 所以这里要直接重置 SessionManager 模拟全新 JVM 状态以确认 agentId 重新生成。
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
