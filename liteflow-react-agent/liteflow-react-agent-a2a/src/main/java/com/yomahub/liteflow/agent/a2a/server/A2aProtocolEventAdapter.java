package com.yomahub.liteflow.agent.a2a.server;

import io.agentscope.core.agent.Event;
import io.agentscope.core.agent.EventType;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentResultEvent;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.message.AssistantMessage;
import reactor.core.publisher.Flux;

/**
 * Bridges typed events to the coarse wire contract still required by AgentScope A2A 2.0.2.
 * Delete this adapter when upstream {@code AgentRunner} accepts {@link AgentEvent} directly.
 */
@SuppressWarnings({"deprecation", "removal"})
final class A2aProtocolEventAdapter {

    Flux<Event> adapt(Flux<AgentEvent> source) {
        return source.concatMap(this::adaptOne);
    }

    private Flux<Event> adaptOne(AgentEvent event) {
        if (event instanceof RequireUserConfirmEvent) {
            return Flux.error(new UnsupportedOperationException(
                    "A2A server does not support inline confirmation across this boundary"));
        }
        if (event instanceof TextBlockDeltaEvent delta) {
            AssistantMessage message = AssistantMessage.builder()
                    .id(delta.getReplyId())
                    .textContent(delta.getDelta())
                    .build();
            return Flux.just(new Event(EventType.REASONING, message, false));
        }
        if (event instanceof AgentResultEvent result) {
            return Flux.just(new Event(EventType.AGENT_RESULT, result.getResult(), true));
        }
        return Flux.empty();
    }
}
