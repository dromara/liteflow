package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.ActingInput;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.middleware.ReasoningInput;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

/** AgentScope 2 middleware logging lifecycle boundaries without retaining invocation state. */
public final class ReActLoggingMiddleware implements MiddlewareBase {

    private static final LFLog LOG = LFLoggerManager.getLogger(ReActLoggingMiddleware.class);
    private final boolean enabled;
    private final Consumer<String> warningSink;

    public ReActLoggingMiddleware(boolean enabled) {
        this(enabled, message -> LOG.warn(message));
    }

    ReActLoggingMiddleware(boolean enabled, Consumer<String> warningSink) {
        this.enabled = enabled;
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
    }

    @Override
    public int order() {
        return AgentMiddlewareOrder.LOGGING;
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext context,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        return observe("agent", context, () -> next.apply(input));
    }

    @Override
    public Flux<AgentEvent> onReasoning(
            Agent agent,
            RuntimeContext context,
            ReasoningInput input,
            Function<ReasoningInput, Flux<AgentEvent>> next) {
        return observe("reasoning", context, () -> next.apply(input));
    }

    @Override
    public Flux<AgentEvent> onActing(
            Agent agent,
            RuntimeContext context,
            ActingInput input,
            Function<ActingInput, Flux<AgentEvent>> next) {
        return observe("acting", context, () -> next.apply(input));
    }

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext context,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        return observe("model", context, () -> next.apply(input));
    }

    private Flux<AgentEvent> observe(
            String phase, RuntimeContext runtimeContext, StreamSupplier source) {
        return Flux.defer(() -> {
            if (!enabled) {
                return source.get();
            }
            LiteFlowAgentContext context = runtimeContext == null
                    ? null
                    : runtimeContext.get(LiteFlowAgentContext.class);
            safeLog(() -> LOG.info(
                    "[agent:{}][{}] >>>",
                    phase,
                    contextLabel(context)));
            Flux<AgentEvent> stream;
            try {
                stream = source.get();
            } catch (Throwable failure) {
                safeError(phase, context, failure);
                return Flux.error(failure);
            }
            if (stream == null) {
                return Flux.error(new IllegalStateException("Agent middleware next returned null"));
            }
            return stream
                    .doOnComplete(() -> safeLog(() -> LOG.info(
                            "[agent:{}][{}] <<<",
                            phase,
                            contextLabel(context))))
                    .doOnError(failure -> safeError(phase, context, failure));
        });
    }

    private void safeError(
            String phase, LiteFlowAgentContext context, Throwable failure) {
        String category = failure == null ? "unknown" : failure.getClass().getName();
        safeLog(() -> warningSink.accept(
                "Agent execution failed category=" + category
                        + " phase=" + phase
                        + " " + contextLabel(context)));
    }

    private static void safeLog(Runnable action) {
        try {
            action.run();
        } catch (Throwable ignored) {
            // Diagnostics never replace or mask agent execution signals.
        }
    }

    static String truncate(String value, int maximumCodePoints) {
        if (value == null || maximumCodePoints < 1) {
            return "";
        }
        String flattened = value.replaceAll("\\s+", " ").trim();
        int count = flattened.codePointCount(0, flattened.length());
        if (count <= maximumCodePoints) {
            return flattened;
        }
        int end = flattened.offsetByCodePoints(0, maximumCodePoints);
        return flattened.substring(0, end) + "...(truncated)";
    }

    static String contextLabel(LiteFlowAgentContext context) {
        if (context == null) {
            return "user=- conversation=- agent=- chain=- node=- request=-";
        }
        return "user=" + safe(context.getUserId())
                + " conversation=" + safe(context.getConversationId())
                + " agent=" + safe(context.getAgentKey())
                + " chain=" + safe(context.getChainId())
                + " node=" + safe(context.getNodeId())
                + " request=" + safe(context.getRequestId());
    }

    private static String safe(String value) {
        return value == null || value.isBlank() ? "-" : truncate(value, 200);
    }

    @FunctionalInterface
    private interface StreamSupplier {
        Flux<AgentEvent> get();
    }
}
