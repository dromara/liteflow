package com.yomahub.liteflow.test.agent.feature.events;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.event.AgentEventTypeMapper;
import com.yomahub.liteflow.agent.middleware.FlowEventBridgeMiddleware;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.FlowEventPublisher;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import com.yomahub.liteflow.test.agent.support.AgentTestEvents;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.AgentInput;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TypedAgentEventsTest {

    @Test
    void bridgePublishesTypedDeltaToolConfirmAndSingleTerminalResult() {
        LiteFlowAgentContext context = AgentTestEvents.context();
        List<FlowEvent> published = new ArrayList<>();
        FlowEventPublisher.setListener(context.getSlot(), published::add);
        ToolUseBlock tool = new ToolUseBlock("tool-1", "lookup", Map.of("q", "offline"));
        List<AgentEvent> source = new ArrayList<>();
        source.addAll(AgentTestEvents.successfulToolRound(
                "reply-1", tool, "delta", "offline result"));
        source.add(1, AgentTestEvents.confirmation("reply-1", tool));

        new FlowEventBridgeMiddleware(AgentListenerFailureMode.FAIL_FAST)
                .onAgent(null, AgentTestEvents.runtimeContext(context),
                        new AgentInput(List.of()), ignored -> Flux.fromIterable(source))
                .collectList()
                .block();

        List<String> types = published.stream().map(FlowEvent::getType).toList();
        assertTrue(types.contains(AgentEventTypeMapper.TEXT_DELTA));
        assertTrue(types.contains(AgentEventTypeMapper.TOOL_CALL_START));
        assertTrue(types.contains(AgentEventTypeMapper.TOOL_CALL_END));
        assertTrue(types.contains(AgentEventTypeMapper.CONFIRM_REQUIRED));
        assertTrue(types.contains(AgentEventTypeMapper.RESULT));
        assertEquals(1, published.stream().filter(FlowEvent::isLast).count());
    }

    @Test
    void bridgePublishesTypedErrorAndPreservesTheSourceFailure() {
        LiteFlowAgentContext context = AgentTestEvents.context();
        List<FlowEvent> published = new ArrayList<>();
        FlowEventPublisher.setListener(context.getSlot(), published::add);
        IllegalStateException failure = new IllegalStateException("offline event failure");

        Throwable thrown = assertThrows(Throwable.class, () ->
                new FlowEventBridgeMiddleware(AgentListenerFailureMode.FAIL_FAST)
                        .onAgent(null, AgentTestEvents.runtimeContext(context),
                                new AgentInput(List.of()), ignored -> Flux.error(failure))
                        .blockLast());

        assertTrue(hasCause(thrown, failure));
        assertEquals(List.of(AgentEventTypeMapper.ERROR),
                published.stream().map(FlowEvent::getType).toList());
        assertTrue(published.get(0).isLast());
    }

    private static boolean hasCause(Throwable thrown, Throwable expected) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current == expected) {
                return true;
            }
        }
        return false;
    }
}
