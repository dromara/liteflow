package com.yomahub.liteflow.test.agent.support;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEndEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolUseBlock;

import java.util.List;
import java.time.Instant;

/** Typed AgentScope 2 event fixtures shared by offline integration tests. */
public final class AgentTestEvents {

    private AgentTestEvents() {
    }

    public static LiteFlowAgentContext context() {
        Slot slot = new Slot();
        slot.setChainId("events-chain");
        slot.setConversationId("events-conversation");
        slot.putRequestId("events-request");
        return new LiteFlowAgentContext(
                new AgentInvocationIdentity(
                        "events-namespace",
                        "events-user",
                        "events-conversation",
                        "events-agent",
                        null,
                        null,
                        null),
                slot,
                "events-chain",
                "events-node",
                "events-request",
                "events-trace",
                Instant.parse("2030-01-01T00:00:00Z"),
                AgentOutputSpec.text(),
                LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX + "events");
    }

    public static RuntimeContext runtimeContext(LiteFlowAgentContext context) {
        return RuntimeContext.builder()
                .userId(context.getRuntimeUserId())
                .sessionId(context.getRuntimeSessionId())
                .put(LiteFlowAgentContext.class, context)
                .put(Slot.class, context.getSlot())
                .build();
    }

    public static List<AgentEvent> successfulToolRound(
            String replyId, ToolUseBlock tool, String delta, String result) {
        Msg reply = AssistantMessage.builder().textContent(result).build();
        return List.of(
                new TextBlockDeltaEvent(replyId, "text-1", delta),
                new ToolCallStartEvent(replyId, tool.getId(), tool.getName()),
                new ToolCallEndEvent(replyId, tool.getId(), tool.getName()),
                new AgentResultEvent(reply),
                new AgentEndEvent(replyId));
    }

    public static RequireUserConfirmEvent confirmation(String replyId, ToolUseBlock tool) {
        return new RequireUserConfirmEvent(replyId, List.of(tool));
    }
}
