package com.yomahub.liteflow.agent.a2a;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.message.UserMessage;

import java.time.Duration;
import java.util.Objects;

/** Immutable inputs for one remote A2A invocation. */
public record A2aClientRequest(
        String remoteAgentName,
        AgentCardResolver resolver,
        A2aAgentConfig config,
        UserMessage message,
        Duration timeout,
        LiteFlowAgentContext context) {

    public A2aClientRequest {
        if (remoteAgentName == null || remoteAgentName.isBlank()) {
            throw new IllegalArgumentException("remoteAgentName must not be blank");
        }
        Objects.requireNonNull(resolver, "resolver");
        Objects.requireNonNull(config, "config");
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(timeout, "timeout");
        Objects.requireNonNull(context, "context");
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive");
        }
    }
}
