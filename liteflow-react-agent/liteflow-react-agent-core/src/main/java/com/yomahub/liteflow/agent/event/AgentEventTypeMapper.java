package com.yomahub.liteflow.agent.event;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.flow.FlowEvent;
import io.agentscope.core.event.AgentEndEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.HintBlockEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ThinkingBlockDeltaEvent;
import io.agentscope.core.event.ToolCallDeltaEvent;
import io.agentscope.core.event.ToolCallEndEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import io.agentscope.core.event.ToolResultDataDeltaEvent;
import io.agentscope.core.event.ToolResultEndEvent;
import io.agentscope.core.event.ToolResultStartEvent;
import io.agentscope.core.event.ToolResultTextDeltaEvent;
import io.agentscope.core.event.UserConfirmResultEvent;

import java.util.List;
import java.util.Map;

/** Maps AgentScope 2 typed events to the stable LiteFlow flow-event vocabulary. */
public final class AgentEventTypeMapper {

    public static final String AGENT_START = "agent.start";
    public static final String AGENT_END = "agent.end";
    public static final String TEXT_DELTA = "agent.text.delta";
    public static final String THINKING_DELTA = "agent.thinking.delta";
    public static final String TOOL_CALL_START = "agent.tool.call.start";
    public static final String TOOL_CALL_DELTA = "agent.tool.call.delta";
    public static final String TOOL_CALL_END = "agent.tool.call.end";
    public static final String TOOL_RESULT_START = "agent.tool.result.start";
    public static final String TOOL_RESULT_DELTA = "agent.tool.result.delta";
    public static final String TOOL_RESULT_END = "agent.tool.result.end";
    public static final String CONFIRM_REQUIRED = "agent.confirm.required";
    public static final String CONFIRM_RESULT = "agent.confirm.result";
    public static final String REASONING = "agent.reasoning";
    public static final String TOOL_RESULT = "agent.tool_result";
    public static final String SUMMARY = "agent.summary";
    public static final String RESULT = "agent.result";
    public static final String ERROR = "agent.error";

    private AgentEventTypeMapper() {
    }

    public static List<FlowEvent> map(AgentEvent event, LiteFlowAgentContext context) {
        if (event instanceof AgentStartEvent) {
            return one(event, context, AGENT_START, null, false);
        }
        if (event instanceof AgentEndEvent) {
            return one(event, context, AGENT_END, null, true);
        }
        if (event instanceof TextBlockDeltaEvent text) {
            return List.of(
                    build(TEXT_DELTA, text.getDelta(), false, event, context),
                    build(REASONING, text.getDelta(), false, event, context));
        }
        if (event instanceof ThinkingBlockDeltaEvent thinking) {
            return one(event, context, THINKING_DELTA, thinking.getDelta(), false);
        }
        if (event instanceof ToolCallStartEvent) {
            return one(event, context, TOOL_CALL_START, null, false);
        }
        if (event instanceof ToolCallDeltaEvent delta) {
            return one(event, context, TOOL_CALL_DELTA, delta.getDelta(), false);
        }
        if (event instanceof ToolCallEndEvent) {
            return one(event, context, TOOL_CALL_END, null, false);
        }
        if (event instanceof ToolResultStartEvent) {
            return one(event, context, TOOL_RESULT_START, null, false);
        }
        if (event instanceof ToolResultTextDeltaEvent delta) {
            return List.of(
                    build(TOOL_RESULT_DELTA, delta.getDelta(), false, event, context),
                    build(TOOL_RESULT, delta.getDelta(), false, event, context));
        }
        if (event instanceof ToolResultDataDeltaEvent) {
            return List.of(
                    build(TOOL_RESULT_DELTA, null, false, event, context),
                    build(TOOL_RESULT, null, false, event, context));
        }
        if (event instanceof ToolResultEndEvent end) {
            return List.of(
                    build(TOOL_RESULT_END, null, false, event, context),
                    build(TOOL_RESULT, end.getToolCallName(), false, event, context));
        }
        if (event instanceof RequireUserConfirmEvent) {
            return one(event, context, CONFIRM_REQUIRED, null, false);
        }
        if (event instanceof UserConfirmResultEvent) {
            return one(event, context, CONFIRM_RESULT, null, false);
        }
        if (event instanceof HintBlockEvent hint) {
            return one(event, context, SUMMARY, hint.getHint(), false);
        }
        if (event instanceof AgentResultEvent result) {
            return one(event, context, RESULT,
                    result.getResult() == null ? null : result.getResult().getTextContent(), true);
        }
        return List.of();
    }

    public static FlowEvent error(Throwable failure, LiteFlowAgentContext context) {
        String text = failure == null ? null : failure.getMessage();
        return build(ERROR, text, true, null, context);
    }

    private static List<FlowEvent> one(
            AgentEvent event,
            LiteFlowAgentContext context,
            String type,
            String text,
            boolean last) {
        return List.of(build(type, text, last, event, context));
    }

    private static FlowEvent build(
            String type,
            String text,
            boolean last,
            AgentEvent event,
            LiteFlowAgentContext context) {
        AgentFlowEventData data = data(event, context);
        return FlowEvent.builder()
                .type(type)
                .chainId(context.getChainId())
                .nodeId(context.getNodeId())
                .requestId(context.getRequestId())
                .conversationId(context.getConversationId())
                .text(text)
                .last(last)
                .data(data)
                .build();
    }

    private static AgentFlowEventData data(
            AgentEvent event, LiteFlowAgentContext context) {
        String taskId = null;
        if (event != null) {
            Map<String, Object> metadata = event.getMetadata();
            Object rawTaskId = metadata == null ? null : metadata.get(AgentEvent.METADATA_TASK_ID);
            taskId = rawTaskId == null ? null : rawTaskId.toString();
        }
        return new AgentFlowEventData(
                event,
                context.getUserId(),
                context.getConversationId(),
                context.getAgentKey(),
                context.getChainId(),
                context.getNodeId(),
                context.getRequestId(),
                context.getTraceId(),
                taskId,
                replyId(event));
    }

    private static String replyId(AgentEvent event) {
        if (event instanceof AgentStartEvent value) return value.getReplyId();
        if (event instanceof AgentEndEvent value) return value.getReplyId();
        if (event instanceof TextBlockDeltaEvent value) return value.getReplyId();
        if (event instanceof ThinkingBlockDeltaEvent value) return value.getReplyId();
        if (event instanceof ToolCallStartEvent value) return value.getReplyId();
        if (event instanceof ToolCallDeltaEvent value) return value.getReplyId();
        if (event instanceof ToolCallEndEvent value) return value.getReplyId();
        if (event instanceof ToolResultStartEvent value) return value.getReplyId();
        if (event instanceof ToolResultTextDeltaEvent value) return value.getReplyId();
        if (event instanceof ToolResultDataDeltaEvent value) return value.getReplyId();
        if (event instanceof ToolResultEndEvent value) return value.getReplyId();
        if (event instanceof RequireUserConfirmEvent value) return value.getReplyId();
        if (event instanceof UserConfirmResultEvent value) return value.getReplyId();
        if (event instanceof HintBlockEvent value) return value.getReplyId();
        return null;
    }
}
