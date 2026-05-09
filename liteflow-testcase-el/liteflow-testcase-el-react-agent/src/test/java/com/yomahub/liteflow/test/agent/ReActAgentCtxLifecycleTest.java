package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.StubReActAgentCmp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 ReActAgentContext 的生命周期边界：
 * 1) agent 跨次复用时，model/工具内通过 comp.ctx() 拿到的是当次 ctx，而非陈旧的构建期 ctx
 * 2) ctx() 在 process() 外调用的行为由 StubReActAgentCmp 已有的静态探针验证
 */
public class ReActAgentCtxLifecycleTest extends AbstractReActAgentSpringbootTest {

    @Test
    @DisplayName("agent 复用时，每次执行 model 拿到的是当次 ctx，callCount 累加且 conversationId 一致")
    public void cachedAgent_freshCtxPerInvocation() {
        // 执行两次同一个 chainId（fixed conversation），agent 应该被缓存
        LiteflowResponse r1 = flowExecutor.execute2Resp("stubAgentChain", "first");
        assertTrue(r1.isSuccess());
        LiteflowResponse r2 = flowExecutor.execute2Resp("stubAgentChain", "second");
        assertTrue(r2.isSuccess());

        // BUILD_MODEL_COUNT 只 +1 说明 model 被复用（agent 被缓存）
        assertEquals(1, StubReActAgentCmp.BUILD_MODEL_COUNT.get(),
                "model 应该只构建一次，第二次复用缓存的 agent");

        // stream 被调了两次，每次都通过 comp.ctx() 取当次 ctx
        assertEquals(2, StubReActAgentCmp.MODEL_PROBES.size(), "应有两次 stream 调用");
        assertEquals(1, StubReActAgentCmp.MODEL_PROBES.get(0).callCount());
        assertEquals(2, StubReActAgentCmp.MODEL_PROBES.get(1).callCount());

        // 两次 ctx 的不变量字段应该完全一致
        assertEquals(StubReActAgentCmp.MODEL_PROBES.get(0).conversationId(),
                StubReActAgentCmp.MODEL_PROBES.get(1).conversationId());
        assertEquals(StubReActAgentCmp.MODEL_PROBES.get(0).agentKey(),
                StubReActAgentCmp.MODEL_PROBES.get(1).agentKey());
        assertEquals(StubReActAgentCmp.MODEL_PROBES.get(0).workspaceDir(),
                StubReActAgentCmp.MODEL_PROBES.get(1).workspaceDir());

        // 但两次的 input messages 应该不同（第二次包含历史）
        assertNotEquals(StubReActAgentCmp.MODEL_PROBES.get(0).inputTexts(),
                StubReActAgentCmp.MODEL_PROBES.get(1).inputTexts(),
                "每次 stream 收到的 messages 应该来自当次 process，不能是首次的陈旧捕获");
    }
}
