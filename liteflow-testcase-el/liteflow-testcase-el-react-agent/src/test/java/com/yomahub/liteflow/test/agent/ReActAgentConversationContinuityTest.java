package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.core.ExecuteOption;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.CollabAgentACmp;
import com.yomahub.liteflow.test.agent.cmp.CollabAgentBCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * 验证 {@code flowExecutor.execute2Resp(chainId, param, ExecuteOption)} 入口
 * 在 conversationId 维度上的语义：
 * <ul>
 *   <li>{@code .autoConversationId()} 让框架自动生成 NanoId 标识，response 可读到；</li>
 *   <li>{@code .conversationId(cid)} 按调用方传入的标识落到 slot；</li>
 *   <li>跨次调用使用同一 conversationId 时，第二次执行能读到第一次写入 workspace 的文件
 *       （即同一段对话的状态被保留），可直接支持"连续对话"场景；</li>
 *   <li>未声明 cid 时不主动写入 slot，由组件按需自行处理（不破坏既有 chain 行为）。</li>
 * </ul>
 */
public class ReActAgentConversationContinuityTest extends AbstractReActAgentSpringbootTest {

    @BeforeEach
    public void resetCollabState() {
        CollabAgentACmp.reset();
        CollabAgentBCmp.reset();
    }

    @Test
    public void testAutoConversationIdGeneratesNanoId() {
        LiteflowResponse response = flowExecutor.execute2Resp("collabAgentChain", "go",
                ExecuteOption.of().autoConversationId());

        Assertions.assertTrue(response.isSuccess());
        String generated = response.getConversationId();
        Assertions.assertNotNull(generated, "autoConversationId() should produce a non-null cid");
        Assertions.assertFalse(generated.isBlank());
        Assertions.assertEquals(generated, CollabAgentACmp.SEEN_CONVERSATION_ID.get());
        Assertions.assertEquals(generated, CollabAgentBCmp.SEEN_CONVERSATION_ID.get());
    }

    @Test
    public void testExplicitConversationIdHonored() {
        String cid = "task-explicit-001";
        LiteflowResponse response = flowExecutor.execute2Resp("collabAgentChain", "go",
                ExecuteOption.of().conversationId(cid));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(cid, response.getConversationId());
        Assertions.assertEquals(cid, CollabAgentACmp.SEEN_CONVERSATION_ID.get());
        Assertions.assertEquals(cid, CollabAgentBCmp.SEEN_CONVERSATION_ID.get());
    }

    /**
     * 同一 conversationId 跨次调用：第二次进入 agent A 时应能看到第一次留下的标记文件。
     */
    @Test
    public void testSameConversationIdSharesWorkspaceAcrossCalls() {
        // 每次运行用唯一 cid，避免上一次 mvn test 残留在 target/wk_root 下的目录干扰断言。
        String cid = "continuity-" + System.nanoTime();

        LiteflowResponse first = flowExecutor.execute2Resp("collabAgentChain", "first",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(first.isSuccess());
        String workspaceFromFirst = CollabAgentACmp.SEEN_WORKSPACE.get();
        Assertions.assertNotNull(workspaceFromFirst);
        Assertions.assertEquals(Boolean.FALSE, CollabAgentACmp.MARKER_EXISTED_BEFORE_WRITE.get(),
                "first call should see no pre-existing marker file");

        CollabAgentACmp.reset();
        CollabAgentBCmp.reset();

        LiteflowResponse second = flowExecutor.execute2Resp("collabAgentChain", "second",
                ExecuteOption.of().conversationId(cid));
        Assertions.assertTrue(second.isSuccess());
        Assertions.assertEquals(cid, second.getConversationId());
        Assertions.assertEquals(workspaceFromFirst, CollabAgentACmp.SEEN_WORKSPACE.get(),
                "second call with same conversationId should resolve to the same workspace dir");
        Assertions.assertEquals(Boolean.TRUE, CollabAgentACmp.MARKER_EXISTED_BEFORE_WRITE.get(),
                "second call should observe the marker written by the previous call - workspace state persists across calls");
        Assertions.assertEquals(CollabAgentACmp.MARKER_CONTENT, CollabAgentBCmp.READ_MARKER.get());
    }

    /**
     * 不在 ExecuteOption 中声明 cid 时（既不调用 .conversationId 也不调用 .autoConversationId），
     * 框架不应主动写入 slot.conversationId，沿用既有 chain 行为。
     * 但 ReActAgentComponent 自己的 resolver 仍会兜底为本次执行生成一个，这是组件层面的契约，
     * 与 FlowExecutor 入口语义独立。
     */
    @Test
    public void testNullExecuteOptionDoesNotForceCidAtExecutorLayer() {
        LiteflowResponse response = flowExecutor.execute2Resp("collabAgentChain", "no-cid",
                ExecuteOption.of()); // 既未 .conversationId 也未 .autoConversationId

        Assertions.assertTrue(response.isSuccess());
        // 在没有显式声明的情况下，由 ReActAgentComponent 默认 resolver 生成 cid 并写回 slot；
        // 因此 response 仍能取到 cid，只是这个 cid 来自组件而非 FlowExecutor 入口。
        Assertions.assertNotNull(response.getConversationId());
    }

    /**
     * rid + cid 同时设置：验证组合不爆炸，所有维度都生效。
     */
    @Test
    public void testRequestIdAndConversationIdComposed() {
        String rid = "req-id-xyz";
        String cid = "conv-id-xyz";
        LiteflowResponse response = flowExecutor.execute2Resp("collabAgentChain", "compose",
                ExecuteOption.of()
                        .requestId(rid)
                        .conversationId(cid));

        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals(rid, response.getRequestId());
        Assertions.assertEquals(cid, response.getConversationId());
    }
}
