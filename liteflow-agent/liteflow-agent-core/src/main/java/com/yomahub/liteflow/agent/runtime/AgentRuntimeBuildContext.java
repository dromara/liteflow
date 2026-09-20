package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.property.agent.AgentConfig;

import java.util.Objects;

/** Stable component-level inputs used while building an Agent runtime. */
public record AgentRuntimeBuildContext(
        AgentConfig agentConfig,
        String agentName,
        String agentKey,
        String agentNamespace) {

    public AgentRuntimeBuildContext {
        Objects.requireNonNull(agentConfig, "agentConfig");
        requireText(agentName, "agentName");
        requireText(agentKey, "agentKey");
        requireText(agentNamespace, "agentNamespace");
    }

    private static void requireText(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
