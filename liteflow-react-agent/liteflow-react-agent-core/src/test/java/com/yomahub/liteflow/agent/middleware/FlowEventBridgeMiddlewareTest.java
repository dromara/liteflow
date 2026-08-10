package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.FlowEventPublisher;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.AgentStartEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.middleware.AgentInput;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FlowEventBridgeMiddlewareTest {

    @Test
    void noListenerSkipsMappingAndPublishingEntirely() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        FlowEventBridgeMiddleware middleware = new FlowEventBridgeMiddleware(
                AgentListenerFailureMode.FAIL_FAST);
        ExplosiveEvent event = new ExplosiveEvent();

        List<AgentEvent> forwarded = middleware.onAgent(
                        null,
                        AgentTestContexts.runtimeContext(context),
                        new AgentInput(List.of()),
                        ignored -> Flux.just(event))
                .collectList()
                .block();

        assertEquals(List.of(event), forwarded);
    }

    @Test
    void failFastPreservesListenerCauseAndDoesNotRecursivelyPublishError() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        RuntimeException listenerFailure = new RuntimeException("listener failed");
        AtomicInteger attempts = new AtomicInteger();
        FlowEventPublisher.setListener(context.getSlot(), event -> {
            attempts.incrementAndGet();
            throw listenerFailure;
        });
        FlowEventBridgeMiddleware middleware = new FlowEventBridgeMiddleware(
                AgentListenerFailureMode.FAIL_FAST);

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> middleware.onAgent(
                                null,
                                AgentTestContexts.runtimeContext(context),
                                new AgentInput(List.of()),
                                ignored -> Flux.just(
                                        new AgentStartEvent("session-1", "reply-1", "agent")))
                        .blockLast());

        assertSame(listenerFailure, thrown);
        assertEquals(1, attempts.get());
    }

    @Test
    void logAndContinueReportsEveryListenerFailureAndPreservesSourceEvents() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        AtomicInteger attempts = new AtomicInteger();
        List<String> warnings = new ArrayList<>();
        FlowEventPublisher.setListener(context.getSlot(), event -> {
            attempts.incrementAndGet();
            throw new IllegalStateException("observer unavailable");
        });
        FlowEventBridgeMiddleware middleware = new FlowEventBridgeMiddleware(
                AgentListenerFailureMode.LOG_AND_CONTINUE,
                warnings::add);
        TextBlockDeltaEvent source = new TextBlockDeltaEvent(
                "reply-1", "block-1", "delta");

        List<AgentEvent> forwarded = middleware.onAgent(
                        null,
                        AgentTestContexts.runtimeContext(context),
                        new AgentInput(List.of()),
                        ignored -> Flux.just(source))
                .collectList()
                .block();

        assertEquals(List.of(source), forwarded);
        assertEquals(2, attempts.get(), "typed text and compatibility reasoning both publish");
        assertEquals(2, warnings.size());
    }

    @Test
    void sourceFailurePublishesOneErrorEventAndStillPropagatesOriginalCause() {
        LiteFlowAgentContext context = AgentTestContexts.liteFlowContext();
        List<FlowEvent> published = new ArrayList<>();
        FlowEventPublisher.setListener(context.getSlot(), published::add);
        FlowEventBridgeMiddleware middleware = new FlowEventBridgeMiddleware(
                AgentListenerFailureMode.FAIL_FAST);
        IllegalArgumentException sourceFailure = new IllegalArgumentException("model failed");

        IllegalArgumentException thrown = assertThrows(
                IllegalArgumentException.class,
                () -> middleware.onAgent(
                                null,
                                AgentTestContexts.runtimeContext(context),
                                new AgentInput(List.of()),
                                ignored -> Flux.error(sourceFailure))
                        .blockLast());

        assertSame(sourceFailure, thrown);
        assertEquals(List.of("agent.error"),
                published.stream().map(FlowEvent::getType).toList());
    }

    private static final class ExplosiveEvent extends AgentEvent {
        @Override
        public AgentEventType getType() {
            throw new AssertionError("event must not be inspected without a listener");
        }
    }
}
