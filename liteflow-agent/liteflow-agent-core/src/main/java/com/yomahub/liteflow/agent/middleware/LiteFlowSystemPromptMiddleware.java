package com.yomahub.liteflow.agent.middleware;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.middleware.MiddlewareBase;
import reactor.core.publisher.Mono;

import java.util.Objects;

/** Applies a call-scoped system-prompt transformation from {@link RuntimeContext}. */
public final class LiteFlowSystemPromptMiddleware implements MiddlewareBase {

    private final SystemPromptTransformer transformer;

    public LiteFlowSystemPromptMiddleware(SystemPromptTransformer transformer) {
        this.transformer = Objects.requireNonNull(transformer, "transformer");
    }

    @Override
    public Mono<String> onSystemPrompt(
            Agent agent, RuntimeContext runtimeContext, String currentPrompt) {
        return Mono.defer(() -> {
            LiteFlowAgentContext context = requireContext(runtimeContext);
            Mono<String> transformed = transformer.transform(currentPrompt, context);
            if (transformed == null) {
                return Mono.error(new AgentConfigException(
                        "transformSystemPrompt must not return null"));
            }
            return transformed.switchIfEmpty(Mono.error(new AgentConfigException(
                    "transformSystemPrompt must not produce null")));
        });
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException(
                    "RuntimeContext is required for dynamic system prompt transformation");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException(
                    "LiteFlowAgentContext is required for dynamic system prompt transformation");
        }
        return context;
    }

    @FunctionalInterface
    public interface SystemPromptTransformer {
        Mono<String> transform(String currentPrompt, LiteFlowAgentContext context);
    }
}
