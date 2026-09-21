package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;

import java.util.Objects;

/** Immutable key for serializing one workspace or one agent state stream. */
public record AgentInvocationKey(
        AgentInvocationScope scope,
        String namespace,
        String conversationId,
        String agentKey) {

    public AgentInvocationKey {
        Objects.requireNonNull(scope, "scope");
        requireValue(namespace, "namespace");
        requireValue(conversationId, "conversationId");
        if (scope == AgentInvocationScope.STATE) {
            requireValue(agentKey, "agentKey");
        } else if (agentKey != null) {
            throw new IllegalArgumentException("conversation/workspace key must not contain agentKey");
        }
    }

    public static AgentInvocationKey workspace(AgentInvocationIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        return workspace(identity.namespace(), identity.conversationId());
    }

    public static AgentInvocationKey workspace(String namespace, String conversationId) {
        return new AgentInvocationKey(AgentInvocationScope.WORKSPACE, namespace, conversationId, null);
    }

    public static AgentInvocationKey state(AgentInvocationIdentity identity) {
        Objects.requireNonNull(identity, "identity");
        return state(identity.namespace(), identity.conversationId(), identity.agentKey());
    }

    public static AgentInvocationKey conversation(String namespace, String conversationId) {
        return new AgentInvocationKey(AgentInvocationScope.CONVERSATION, namespace, conversationId, null);
    }

    public static AgentInvocationKey state(String namespace, String conversationId, String agentKey) {
        return new AgentInvocationKey(AgentInvocationScope.STATE, namespace, conversationId, agentKey);
    }

    private static void requireValue(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }
}
