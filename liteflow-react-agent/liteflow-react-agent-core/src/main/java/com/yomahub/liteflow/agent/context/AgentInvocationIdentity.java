package com.yomahub.liteflow.agent.context;

import java.util.Objects;

/** Raw and safe identifiers for one agent invocation. */
public record AgentInvocationIdentity(
        String namespace,
        String userId,
        String conversationId,
        String agentKey,
        String runtimeSessionId,
        String agentNamespace,
        String storeSessionId) {

    public AgentInvocationIdentity {
        requireValue(namespace, "namespace");
        requireValue(userId, "userId");
        requireValue(conversationId, "conversationId");
        requireValue(agentKey, "agentKey");
        requireValue(runtimeSessionId, "runtimeSessionId");
        requireValue(agentNamespace, "agentNamespace");
        requireValue(storeSessionId, "storeSessionId");
    }

    static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
