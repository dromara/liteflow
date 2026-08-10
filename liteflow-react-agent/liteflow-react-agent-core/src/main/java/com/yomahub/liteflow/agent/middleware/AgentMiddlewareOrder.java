package com.yomahub.liteflow.agent.middleware;

import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.middleware.ReasoningInput;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Objects;
import java.util.function.Function;

/** Stable priorities for LiteFlow-owned AgentScope middleware. */
public final class AgentMiddlewareOrder {

    public static final int STATE_STORE_FAILURE = 10_000;
    public static final int LOGGING = 9_000;
    public static final int FLOW_EVENT = 8_000;
    public static final int USAGE_SKILL = 7_000;
    public static final int USER = 1_000;

    private AgentMiddlewareOrder() {
    }

    /**
     * Normalizes caller middleware into the user layer without changing the caller's object.
     */
    public static MiddlewareBase user(MiddlewareBase middleware) {
        return new OrderedUserMiddleware(Objects.requireNonNull(middleware, "middleware"));
    }

    private static final class OrderedUserMiddleware implements MiddlewareBase {
        private final MiddlewareBase delegate;

        private OrderedUserMiddleware(MiddlewareBase delegate) {
            this.delegate = delegate;
        }

        @Override
        public int order() {
            return USER;
        }

        @Override
        public Flux<AgentEvent> onAgent(
                Agent agent,
                RuntimeContext context,
                AgentInput input,
                Function<AgentInput, Flux<AgentEvent>> next) {
            return delegate.onAgent(agent, context, input, next);
        }

        @Override
        public Flux<AgentEvent> onReasoning(
                Agent agent,
                RuntimeContext context,
                ReasoningInput input,
                Function<ReasoningInput, Flux<AgentEvent>> next) {
            return delegate.onReasoning(agent, context, input, next);
        }

        @Override
        public Flux<AgentEvent> onActing(
                Agent agent,
                RuntimeContext context,
                ActingInput input,
                Function<ActingInput, Flux<AgentEvent>> next) {
            return delegate.onActing(agent, context, input, next);
        }

        @Override
        public Flux<AgentEvent> onModelCall(
                Agent agent,
                RuntimeContext context,
                ModelCallInput input,
                Function<ModelCallInput, Flux<AgentEvent>> next) {
            return delegate.onModelCall(agent, context, input, next);
        }

        @Override
        public Mono<String> onSystemPrompt(
                Agent agent, RuntimeContext context, String currentPrompt) {
            return delegate.onSystemPrompt(agent, context, currentPrompt);
        }
    }
}
