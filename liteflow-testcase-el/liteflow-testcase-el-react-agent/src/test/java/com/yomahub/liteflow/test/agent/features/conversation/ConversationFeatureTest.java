package com.yomahub.liteflow.test.agent.features.conversation;

import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import com.yomahub.liteflow.test.agent.features.support.ReActAgentFeatureTestSupport;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.Map;

/**
 * 覆盖 guide 中 conversationId、agentKey 与 workspace 的协作边界。
 *
 * <p>链路使用 {@code THEN(agentA, agentB)}，两个 Agent 默认使用不同 nodeId 作为
 * agentKey，但会继承同一个 conversationId，因此应共享同一个 workspace 子目录。
 */
@TestPropertySource(value = "classpath:/agent/features/conversation/application.properties")
@SpringBootTest(classes = ConversationFeatureTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.agent.features.conversation.cmp" })
public class ConversationFeatureTest {

    private static final String RAW_CONVERSATION_ID = "chat/user 1";

    @Resource
    private FlowExecutor flowExecutor;

    @Resource
    private LiteflowConfig liteflowConfig;

    @BeforeEach
    public void reset() throws Exception {
        ReActAgentFeatureTestSupport.ensureAgentConfig(
                liteflowConfig,
                "target/wk_react_agent_conversation",
                false,
                null,
                ShellMode.DISABLED);
        ReActAgentFeatureTestSupport.resetAgentSessionManager();
        CompatibleCustomEchoAgentComponent.resetCompatibleProbe();
        ConversationFeatureProbe.reset();
    }

    @Test
    public void testConversationIdIsSanitizedAndSharedAcrossThenAgents() {
        LiteflowResponse response = flowExecutor.execute2Resp("conversationFeatureChain", Map.of(
                ReActAgentComponent.CONVERSATION_ID_REQUEST_KEY, RAW_CONVERSATION_ID,
                "prompt", "share workspace"));

        Assertions.assertTrue(response.isSuccess(), response.getMessage());
        Assertions.assertEquals(2, CompatibleCustomEchoAgentComponent.COMPATIBLE_SPEC_RESOLVE_COUNT.get(),
                "两个不同 agentKey 的 Agent 应分别构建各自的 compatible-custom 模型");
        Assertions.assertNotEquals(RAW_CONVERSATION_ID, ConversationFeatureProbe.AGENT_A_CONVERSATION_ID.get(),
                "ctx 中的 conversationId 应使用安全化后的目录名");
        Assertions.assertEquals(ConversationFeatureProbe.AGENT_A_CONVERSATION_ID.get(),
                ConversationFeatureProbe.AGENT_B_CONVERSATION_ID.get(),
                "同一条 THEN 链路内后续 Agent 应复用首个 Agent 的 conversation");
        Assertions.assertEquals(ConversationFeatureProbe.AGENT_A_WORKSPACE.get(),
                ConversationFeatureProbe.AGENT_B_WORKSPACE.get(),
                "同一 conversation 下的多个 agentKey 应共享 workspace");
        Assertions.assertEquals("conversationAgentA", ConversationFeatureProbe.AGENT_A_KEY.get());
        Assertions.assertEquals("conversationAgentB", ConversationFeatureProbe.AGENT_B_KEY.get());
    }
}
