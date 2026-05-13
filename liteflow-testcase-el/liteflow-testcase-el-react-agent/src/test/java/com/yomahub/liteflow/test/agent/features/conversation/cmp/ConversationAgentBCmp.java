package com.yomahub.liteflow.test.agent.features.conversation.cmp;

import com.yomahub.liteflow.test.agent.features.conversation.ConversationFeatureProbe;
import com.yomahub.liteflow.test.agent.features.support.CompatibleCustomEchoAgentComponent;
import org.springframework.stereotype.Component;

/**
 * 第二个 conversation Agent，不覆写 resolveConversationId()，用于验证 slot 中的 conversation 复用。
 */
@Component("conversationAgentB")
public class ConversationAgentBCmp extends CompatibleCustomEchoAgentComponent {
    @Override
    protected String userPrompt() {
        ConversationFeatureProbe.AGENT_B_CONVERSATION_ID.set(ctx().getConversationId());
        ConversationFeatureProbe.AGENT_B_KEY.set(ctx().getAgentKey());
        ConversationFeatureProbe.AGENT_B_WORKSPACE.set(ctx().getWorkspaceDir().toString());
        return super.userPrompt();
    }
}
