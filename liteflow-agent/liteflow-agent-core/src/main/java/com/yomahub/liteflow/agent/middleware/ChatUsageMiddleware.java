package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.ModelCallEndEvent;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import reactor.core.publisher.Flux;

import java.util.function.Function;

/** Aggregates usage into the current invocation context. */
public final class ChatUsageMiddleware implements MiddlewareBase {

    @Override
    public int order() {
        return AgentMiddlewareOrder.USAGE_SKILL;
    }

    @Override
    public Flux<AgentEvent> onModelCall(
            Agent agent,
            RuntimeContext runtimeContext,
            ModelCallInput input,
            Function<ModelCallInput, Flux<AgentEvent>> next) {
        return Flux.defer(() -> {
            LiteFlowAgentContext context = requireContext(runtimeContext);
            return next.apply(input).doOnNext(event -> {
                if (event instanceof ModelCallEndEvent end && end.getUsage() != null) {
                    context.recordChatUsage(end.getId(), end.getUsage());
                }
            });
        });
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException("RuntimeContext is required for usage tracking");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException("LiteFlowAgentContext is required for usage tracking");
        }
        return context;
    }
}
