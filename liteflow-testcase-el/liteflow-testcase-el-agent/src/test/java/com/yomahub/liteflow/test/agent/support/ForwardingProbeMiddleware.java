package com.yomahub.liteflow.test.agent.support;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ReasoningInput;
import reactor.core.publisher.Flux;

import java.util.function.Function;
import java.util.function.Supplier;

/** Stable runtime middleware that resolves the current test probe for every callback. */
public final class ForwardingProbeMiddleware implements MiddlewareBase {

    private final Supplier<MiddlewareBase> delegateSupplier;

    public ForwardingProbeMiddleware(Supplier<MiddlewareBase> delegateSupplier) {
        this.delegateSupplier = delegateSupplier;
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext context,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        MiddlewareBase delegate = delegateSupplier.get();
        return delegate == null ? next.apply(input) : delegate.onReasoning(agent, context, input, next);
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext context,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        MiddlewareBase delegate = delegateSupplier.get();
        return delegate == null ? next.apply(input) : delegate.onActing(agent, context, input, next);
    }
}
