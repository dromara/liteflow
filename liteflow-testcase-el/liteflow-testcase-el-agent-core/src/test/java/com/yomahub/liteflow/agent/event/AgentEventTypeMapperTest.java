package com.yomahub.liteflow.agent.event;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.flow.FlowEvent;
import io.agentscope.core.event.AgentEndEvent;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.ConfirmResult;
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
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class AgentEventTypeMapperTest {

    @Test
    void mapsTypedAndCompatibilityEventsAtTheirDocumentedBoundaries() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        ToolUseBlock tool = new ToolUseBlock("tool-1", "lookup", Map.of("q", "literal"));
        Msg result = AssistantMessage.builder().textContent("final answer").build();
        List<EventCase> cases = List.of(
                new EventCase(new AgentStartEvent("session-1", "reply-1", "agent"),
                        List.of("agent.start"), "reply-1"),
                new EventCase(new AgentEndEvent("reply-1"), List.of("agent.end"), "reply-1"),
                new EventCase(new TextBlockDeltaEvent("reply-1", "block-1", "hello"),
                        List.of("agent.text.delta", "agent.reasoning"), "reply-1"),
                new EventCase(new ThinkingBlockDeltaEvent("reply-1", "block-2", "brief"),
                        List.of("agent.thinking.delta"), "reply-1"),
                new EventCase(new ToolCallStartEvent("reply-1", "tool-1", "lookup"),
                        List.of("agent.tool.call.start"), "reply-1"),
                new EventCase(new ToolCallDeltaEvent(
                        "reply-1", "tool-1", "lookup", "{\"q\":"),
                        List.of("agent.tool.call.delta"), "reply-1"),
                new EventCase(new ToolCallEndEvent("reply-1", "tool-1", "lookup"),
                        List.of("agent.tool.call.end"), "reply-1"),
                new EventCase(new ToolResultStartEvent("reply-1", "tool-1", "lookup"),
                        List.of("agent.tool.result.start"), "reply-1"),
                new EventCase(new ToolResultTextDeltaEvent(
                        "reply-1", "tool-1", "lookup", "value"),
                        List.of("agent.tool.result.delta", "agent.tool_result"), "reply-1"),
                new EventCase(new ToolResultDataDeltaEvent(
                        "reply-1", "tool-1", "lookup",
                        TextBlock.builder().text("binary-fixture").build()),
                        List.of("agent.tool.result.delta", "agent.tool_result"), "reply-1"),
                new EventCase(new ToolResultEndEvent(
                        "reply-1", "tool-1", "lookup", ToolResultState.SUCCESS),
                        List.of("agent.tool.result.end", "agent.tool_result"), "reply-1"),
                new EventCase(new RequireUserConfirmEvent("reply-1", List.of(tool)),
                        List.of("agent.confirm.required"), "reply-1"),
                new EventCase(new UserConfirmResultEvent(
                        "reply-1", List.of(new ConfirmResult(true, tool))),
                        List.of("agent.confirm.result"), "reply-1"),
                new EventCase(new HintBlockEvent("reply-1", "hint-1", "system", "summary"),
                        List.of("agent.summary"), "reply-1"),
                new EventCase(new AgentResultEvent(result), List.of("agent.result"), null));

        for (EventCase eventCase : cases) {
            eventCase.event().withMetadataEntry(AgentEvent.METADATA_TASK_ID, "task-1");
            List<FlowEvent> mapped = AgentEventTypeMapper.map(eventCase.event(), context);
            assertEquals(eventCase.expectedTypes(), mapped.stream().map(FlowEvent::getType).toList());
            for (FlowEvent flowEvent : mapped) {
                AgentFlowEventData data = (AgentFlowEventData) flowEvent.getData();
                assertSame(eventCase.event(), data.event());
                assertEquals("user-1", data.userId());
                assertEquals("conversation-1", data.conversationId());
                assertEquals("agent-1", data.agentKey());
                assertEquals("chain-1", data.chainId());
                assertEquals("node-1", data.nodeId());
                assertEquals("request-1", data.requestId());
                assertEquals("trace-1", data.traceId());
                assertEquals("task-1", data.taskId());
                assertEquals(eventCase.expectedReplyId(), data.replyId());
            }
        }

        assertEquals(1,
                AgentEventTypeMapper.map(new AgentResultEvent(result), context).stream()
                        .filter(event -> "agent.result".equals(event.getType()))
                        .count(),
                "typed and compatibility result are the same semantic event");

        FlowEvent compatibleToolEnd = AgentEventTypeMapper.map(
                new ToolResultEndEvent(
                        "reply-1", "tool-1", "lookup", ToolResultState.SUCCESS),
                context).get(1);
        assertEquals("agent.tool_result", compatibleToolEnd.getType());
        assertEquals("lookup", compatibleToolEnd.getText());
        assertFalse(compatibleToolEnd.isLast(),
                "only the final agent.result closes the compatibility stream");
    }

    @Test
    void createsOneErrorEventWithoutFabricatingAnAgentScopeEvent() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        IllegalStateException failure = new IllegalStateException("model failed");

        FlowEvent mapped = AgentEventTypeMapper.error(failure, context);

        assertEquals("agent.error", mapped.getType());
        assertEquals("model failed", mapped.getText());
        AgentFlowEventData data = (AgentFlowEventData) mapped.getData();
        assertNull(data.event());
        assertNull(data.replyId());
    }

    @Test
    void resultThenEndExposesExactlyOneTerminalBoundary() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        Msg result = AssistantMessage.builder().textContent("final answer").build();
        List<FlowEvent> sequence = new ArrayList<>();
        sequence.addAll(AgentEventTypeMapper.map(new AgentResultEvent(result), context));
        sequence.addAll(AgentEventTypeMapper.map(new AgentEndEvent("reply-1"), context));

        List<FlowEvent> terminal = sequence.stream().filter(FlowEvent::isLast).toList();

        assertEquals(1, terminal.size());
        assertEquals("agent.result", terminal.get(0).getType());
    }

    private record EventCase(
            AgentEvent event, List<String> expectedTypes, String expectedReplyId) {
    }
}
