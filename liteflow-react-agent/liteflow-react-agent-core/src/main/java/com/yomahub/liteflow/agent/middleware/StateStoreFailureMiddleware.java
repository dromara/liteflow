package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.agent.AgentStateStoreFailurePolicy;
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
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

/** Converts AgentScope's load-error fallback into the configured LiteFlow failure policy. */
public final class StateStoreFailureMiddleware implements MiddlewareBase {

    private final GuardedNamespacedAgentStateStore stateStore;
    private final AgentStateStoreFailurePolicy failurePolicy;
    private final Consumer<String> warningSink;

    public StateStoreFailureMiddleware(
            GuardedNamespacedAgentStateStore stateStore,
            AgentStateStoreFailurePolicy failurePolicy) {
        this(stateStore, failurePolicy,
                message -> LFLoggerManager.getLogger(StateStoreFailureMiddleware.class)
                        .warn(message));
    }

    public StateStoreFailureMiddleware(
            GuardedNamespacedAgentStateStore stateStore,
            AgentStateStoreFailurePolicy failurePolicy,
            Consumer<String> warningSink) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        if (failurePolicy == null) {
            throw new AgentConfigException(
                    "liteflow.agent.state-store.failure-policy must not be null");
        }
        this.failurePolicy = failurePolicy;
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
    }

    @Override
    public int order() {
        return Integer.MAX_VALUE;
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext context,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> next.apply(input))
                .doFinally(signal -> clearLoadFailure(context));
    }

    @Override
    public Mono<String> onSystemPrompt(
            Agent agent, RuntimeContext context, String currentPrompt) {
        return checkLoadFailure(context).thenReturn(currentPrompt);
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext context,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        return checkLoadFailure(context).thenMany(Flux.defer(() -> next.apply(input)));
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext context,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        return checkLoadFailure(context).thenMany(Flux.defer(() -> next.apply(input)));
    }

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext context,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        return checkLoadFailure(context).thenMany(Flux.defer(() -> next.apply(input)));
    }

    private Mono<Void> checkLoadFailure(RuntimeContext context) {
        return Mono.defer(() -> {
            if (context == null) {
                return Mono.error(new AgentException(
                        "RuntimeContext is required for StateStore failure handling"));
            }
            Optional<Throwable> failure = stateStore.takeLoadFailure(
                    context.getUserId(), context.getSessionId());
            if (failure.isEmpty()) {
                return Mono.empty();
            }
            Throwable cause = failure.orElseThrow();
            String message = "AgentStateStore load failed for userId=" + context.getUserId()
                    + ", sessionId=" + context.getSessionId() + ": " + cause.getMessage();
            if (failurePolicy == AgentStateStoreFailurePolicy.FAIL_FAST) {
                return Mono.error(new AgentException(message, cause));
            }
            warningSink.accept(message);
            return Mono.empty();
        });
    }

    private void clearLoadFailure(RuntimeContext context) {
        if (context != null && context.getSessionId() != null) {
            stateStore.clearLoadFailure(context.getUserId(), context.getSessionId());
        }
    }
}
