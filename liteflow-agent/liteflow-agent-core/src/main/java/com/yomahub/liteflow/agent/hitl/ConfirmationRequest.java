package com.yomahub.liteflow.agent.hitl;

import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.ToolUseBlock;

import java.util.List;
import java.util.Objects;

/** Immutable, correlated snapshot of one permission-confirmation pause. */
public record ConfirmationRequest(
        String replyId,
        RequireUserConfirmEvent event,
        List<ToolUseBlock> toolCalls) {

    public ConfirmationRequest {
        Objects.requireNonNull(replyId, "replyId");
        Objects.requireNonNull(event, "event");
        toolCalls = List.copyOf(Objects.requireNonNull(toolCalls, "toolCalls"));
    }
}
