package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.event.AgentEventTypeMapper;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.FlowEventPublisher;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Flux;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/** Publishes AgentScope 2 typed events to the listener on the current LiteFlow Slot. */
public final class FlowEventBridgeMiddleware implements MiddlewareBase {

    private final AgentListenerFailureMode failureMode;
    private final Consumer<String> warningSink;

    public FlowEventBridgeMiddleware(AgentListenerFailureMode failureMode) {
        this(failureMode, message ->
                LFLoggerManager.getLogger(FlowEventBridgeMiddleware.class).warn(message));
    }

    FlowEventBridgeMiddleware(
            AgentListenerFailureMode failureMode, Consumer<String> warningSink) {
        this.failureMode = Objects.requireNonNull(failureMode, "failureMode");
        this.warningSink = Objects.requireNonNull(warningSink, "warningSink");
    }

    @Override
    public int order() {
        return AgentMiddlewareOrder.FLOW_EVENT;
    }

    @Override
    public Flux<AgentEvent> onAgent(
            Agent agent,
            RuntimeContext runtimeContext,
            AgentInput input,
            Function<AgentInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> {
            LiteFlowAgentContext context = requireContext(runtimeContext);
            AtomicReference<Throwable> listenerFailure = new AtomicReference<>();
            Flux<AgentEvent> source;
            try {
                source = Objects.requireNonNull(next.apply(input), "next returned null");
            } catch (Throwable sourceFailure) {
                return publishSourceFailure(sourceFailure, context, listenerFailure);
            }
            return source
                    .doOnNext(event -> publishEvent(event, context, listenerFailure))
                    .onErrorResume(failure -> {
                        if (listenerFailure.get() == failure) {
                            return Flux.error(failure);
                        }
                        return publishSourceFailure(failure, context, listenerFailure);
                    });
        });
    }

    private Flux<AgentEvent> publishSourceFailure(
            Throwable sourceFailure,
            LiteFlowAgentContext context,
            AtomicReference<Throwable> listenerFailure) {
        if (FlowEventPublisher.hasListener(context.getSlot())) {
            try {
                publish(AgentEventTypeMapper.error(sourceFailure, context), context);
            } catch (RuntimeException | Error deliveryFailure) {
                if (failureMode == AgentListenerFailureMode.FAIL_FAST) {
                    listenerFailure.set(deliveryFailure);
                    return Flux.error(deliveryFailure);
                }
                warn(deliveryFailure);
            }
        }
        return Flux.error(sourceFailure);
    }

    private void publishEvent(
            AgentEvent source,
            LiteFlowAgentContext context,
            AtomicReference<Throwable> listenerFailure) {
        if (!FlowEventPublisher.hasListener(context.getSlot())) {
            return;
        }
        for (FlowEvent event : AgentEventTypeMapper.map(source, context)) {
            try {
                publish(event, context);
            } catch (RuntimeException | Error deliveryFailure) {
                if (failureMode == AgentListenerFailureMode.FAIL_FAST) {
                    listenerFailure.set(deliveryFailure);
                    throw deliveryFailure;
                }
                warn(deliveryFailure);
            }
        }
    }

    private static void publish(FlowEvent event, LiteFlowAgentContext context) {
        FlowEventPublisher.publish(context.getSlot(), event);
    }

    private void warn(Throwable failure) {
        try {
            warningSink.accept("Agent flow-event listener failed: " + failure);
        } catch (Throwable ignored) {
            // Logging must never replace the model or listener failure being handled.
        }
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException("RuntimeContext is required for flow-event delivery");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException(
                    "LiteFlowAgentContext is required for flow-event delivery");
        }
        return context;
    }
}
