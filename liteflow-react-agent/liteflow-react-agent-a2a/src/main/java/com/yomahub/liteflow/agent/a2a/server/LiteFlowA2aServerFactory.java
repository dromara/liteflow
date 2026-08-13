package com.yomahub.liteflow.agent.a2a.server;

import io.agentscope.core.a2a.server.AgentScopeA2aServer;
import io.agentscope.core.a2a.server.card.ConfigurableAgentCard;
import io.agentscope.core.a2a.server.executor.runner.AgentRunner;
import io.agentscope.core.a2a.server.transport.TransportProperties;

import java.util.Objects;

/** Builds the protocol boundary without binding a Web endpoint or publishing readiness. */
public final class LiteFlowA2aServerFactory {

    public AgentScopeA2aServer create(
            AgentRunner agentRunner,
            ConfigurableAgentCard agentCard,
            TransportProperties transportProperties) {
        return AgentScopeA2aServer.builder(
                        Objects.requireNonNull(agentRunner, "agentRunner"))
                .agentCard(Objects.requireNonNull(agentCard, "agentCard"))
                .withTransport(Objects.requireNonNull(
                        transportProperties, "transportProperties"))
                .build();
    }
}
