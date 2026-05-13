package com.yomahub.liteflow.test.agent.features.conversation.cmp;

import com.yomahub.liteflow.test.agent.features.conversation.ConversationFeatureProbe;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import org.springframework.stereotype.Component;

/**
 * 第一个 conversation Agent，负责触发 conversation 解析并记录安全化后的上下文。
 */
@Component("conversationAgentA")
public class ConversationAgentACmp extends CompatibleCustomEchoAgentComponent {
    @Override
    protected String userPrompt() {
        ConversationFeatureProbe.AGENT_A_CONVERSATION_ID.set(ctx().getConversationId());
        ConversationFeatureProbe.AGENT_A_KEY.set(ctx().getAgentKey());
        ConversationFeatureProbe.AGENT_A_WORKSPACE.set(ctx().getWorkspaceDir().toString());
        return super.userPrompt();
    }
}
