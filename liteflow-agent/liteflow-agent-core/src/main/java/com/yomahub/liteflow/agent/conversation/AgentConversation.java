package com.yomahub.liteflow.agent.conversation;

import java.util.Map;

/** Business-facing conversation metadata. Listing conversations never loads their messages. */
public record AgentConversation(String id, String title, long createdAt, long updatedAt,
                                long messageCount, Map<String, String> attributes) {
    public AgentConversation {
        attributes = attributes == null ? Map.of() : Map.copyOf(attributes);
    }
}
