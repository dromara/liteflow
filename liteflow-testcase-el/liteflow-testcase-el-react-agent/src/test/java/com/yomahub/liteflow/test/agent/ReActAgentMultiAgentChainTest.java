package com.yomahub.liteflow.test.agent;

import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.agent.cmp.CollabAgentACmp;
import com.yomahub.liteflow.test.agent.cmp.CollabAgentBCmp;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

/**
 * 验证一条 chain 内编排多个 ReAct Agent 时的会话/工作区行为：
 * <ul>
 *   <li>整个 chain 共享同一个 conversationId（首个 agent 通过默认 resolver 从 requestData 中解析后
 *       通过 {@code slot.setConversationId} 写回，后续 agent 直接读取）；</li>
 *   <li>workspace 目录按 conversationId 创建一次，多个 agent 共享，可以彼此读取对方写入的文件；</li>
 *   <li>每个 agent 的 {@code agentKey}（默认为 nodeId）不同，进而拥有独立的 ReActAgent 实例与持久化记忆 key。</li>
 * </ul>
 */
public class ReActAgentMultiAgentChainTest extends AbstractReActAgentSpringbootTest {

    private static final String CONV_ID = "collab-conv-1024";

    @BeforeEach
    public void resetCollabState() {
        CollabAgentACmp.reset();
        CollabAgentBCmp.reset();
    }

    @Test
    public void testMultipleAgentsShareWorkspaceButHaveDistinctAgentKeys() {
        Map<String, Object> req = Map.of("conversationId", CONV_ID, "userInput", "go");
        LiteflowResponse response = flowExecutor.execute2Resp("collabAgentChain", req);

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));

        // 1. chain 整体共享 conversationId（来自请求 Map 中的 "conversationId"）
        Assertions.assertEquals(CONV_ID, response.getConversationId(),
                "conversationId in request map should propagate to LiteflowResponse");
        Assertions.assertEquals(CONV_ID, CollabAgentACmp.SEEN_CONVERSATION_ID.get(),
                "first agent should resolve conversationId from request data");
        Assertions.assertEquals(CONV_ID, CollabAgentBCmp.SEEN_CONVERSATION_ID.get(),
                "second agent should reuse the same conversationId via slot");

        // 2. workspace 共享：A 和 B 看到的 workspace 路径一致
        Assertions.assertNotNull(CollabAgentACmp.SEEN_WORKSPACE.get());
        Assertions.assertEquals(CollabAgentACmp.SEEN_WORKSPACE.get(), CollabAgentBCmp.SEEN_WORKSPACE.get(),
                "both agents in the same conversation should resolve to the same workspace dir");

        // 3. agentKey 不同：默认对应各自的 nodeId
        Assertions.assertEquals("collabAgentA", CollabAgentACmp.SEEN_AGENT_KEY.get());
        Assertions.assertEquals("collabAgentB", CollabAgentBCmp.SEEN_AGENT_KEY.get());

        // 4. 文件协作：A 写入的 marker 在 B 中可读
        Assertions.assertEquals(CollabAgentACmp.MARKER_CONTENT, CollabAgentBCmp.READ_MARKER.get(),
                "agent B should be able to read the marker file written by agent A in the shared workspace");
    }

    @Test
    public void testDifferentConversationIdsGetSeparateWorkspaces() {
        flowExecutor.execute2Resp("collabAgentChain",
                Map.of("conversationId", "convX", "userInput", "x"));
        String workspaceX = CollabAgentACmp.SEEN_WORKSPACE.get();

        flowExecutor.execute2Resp("collabAgentChain",
                Map.of("conversationId", "convY", "userInput", "y"));
        String workspaceY = CollabAgentACmp.SEEN_WORKSPACE.get();

        Assertions.assertNotNull(workspaceX);
        Assertions.assertNotNull(workspaceY);
        Assertions.assertNotEquals(workspaceX, workspaceY,
                "different conversationIds must resolve to different workspace directories");
    }
}
