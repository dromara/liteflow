package com.yomahub.liteflow.agent.conversation;

import java.util.List;

/** Start at cursor zero; pass nextCursor to retrieve the following page. */
public record AgentConversationPage<T>(List<T> items, long nextCursor, boolean hasMore) {
    public AgentConversationPage {
        items = List.copyOf(items);
    }
}
