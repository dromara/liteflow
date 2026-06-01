package com.yomahub.liteflow.test.agent.feature.multiagent;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
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

import java.util.Map;

/**
 * 覆盖 guide §5 / §8 多 Agent 协作：
 * <ul>
 *   <li>同一条 THEN 链路内两个 Agent 共享 conversationId；</li>
 *   <li>workspace 目录按 conversationId 创建一次，多个 Agent 共享；</li>
 *   <li>各自的 agentKey（默认 nodeId）不同，各自独立 ReActAgent；</li>
 *   <li>各自把回复写到 slot output，互不覆盖。</li>
 * </ul>
 */
@TestPropertySource("classpath:/feature/multiagent/application.properties")
@SpringBootTest(classes = MultiAgentChainTest.class)
@EnableAutoConfiguration
@ComponentScan("com.yomahub.liteflow.test.agent.feature.multiagent")
public class MultiAgentChainTest extends BaseAgentLiveTest {

    private static final String CID = "multi-agent-conv";

    @BeforeEach
    public void resetProbes() {
        MultiAgentACmp.reset();
        MultiAgentBCmp.reset();
        LiveTestSupport.applyCompatibleCustomOrSkip(liteflowConfig, "MultiAgentChainTest");
    }

    @Test
    public void testTwoAgentsShareConversationAndWorkspaceButHaveDistinctAgentKeys() {
        LiteflowResponse response = flowExecutor.execute2Resp("multiAgentChain", Map.of(
                ReActAgentComponent.CONVERSATION_ID_REQUEST_KEY, CID,
                "prompt", "请用一句话作答。"));

        Assertions.assertTrue(response.isSuccess(),
                "chain failed: " + (response.getCause() == null ? "" : response.getCause().getMessage()));
        Assertions.assertEquals(CID, response.getConversationId(),
                "request map 中的 conversationId 应通过默认 resolver 流转到 LiteflowResponse");
        Assertions.assertEquals(CID, MultiAgentACmp.SEEN_CONVERSATION_ID.get());
        Assertions.assertEquals(CID, MultiAgentBCmp.SEEN_CONVERSATION_ID.get());

        Assertions.assertEquals("multiAgentA", MultiAgentACmp.SEEN_AGENT_KEY.get());
        Assertions.assertEquals("multiAgentB", MultiAgentBCmp.SEEN_AGENT_KEY.get());

        // workspace 共享：A 写入的 marker 在 B 中可读。
        Assertions.assertEquals(MultiAgentACmp.SEEN_WORKSPACE.get(), MultiAgentBCmp.SEEN_WORKSPACE.get(),
                "两个 Agent 应解析到同一个 workspace 目录");
        Assertions.assertEquals(MultiAgentACmp.MARKER_CONTENT, MultiAgentBCmp.READ_MARKER.get(),
                "Agent B 应能读到 Agent A 写入的标记文件");

        // 两个 Agent 各自把回复写到 slot output；下游节点不会被覆盖。
        Assertions.assertNotNull(response.getSlot().getOutput("multiAgentA"));
        Assertions.assertNotNull(response.getSlot().getOutput("multiAgentB"));
    }
}
