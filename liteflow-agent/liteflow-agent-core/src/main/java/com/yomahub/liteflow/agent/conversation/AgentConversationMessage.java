package com.yomahub.liteflow.agent.conversation;

import io.agentscope.core.state.State;

/** An immutable display message, independent of the model's compactable working context. */
public record AgentConversationMessage(long sequence, String id, String role, String stage,
                                       String content, String agentKey, String requestId,
                                       long timestamp) implements State {
}
