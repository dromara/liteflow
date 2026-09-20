package com.yomahub.liteflow.agent.event;

import io.agentscope.core.event.AgentEvent;

/** Immutable correlation data attached to a LiteFlow event bridged from AgentScope. */
public record AgentFlowEventData(
        AgentEvent event,
        String conversationId,
        String agentKey,
        String chainId,
        String nodeId,
        String requestId,
        String traceId,
        String taskId,
        String replyId) {
}
