package com.yomahub.liteflow.agent.context;

/** Generates collision-resistant AgentScope runtime and store identifiers. */
public final class InvocationIdentityResolver {

    private final String namespace;

    public InvocationIdentityResolver(String namespace) {
        AgentInvocationIdentity.requireValue(namespace, "namespace");
        this.namespace = namespace;
    }

    public AgentInvocationIdentity resolve(String userId, String conversationId, String agentKey) {
        AgentInvocationIdentity.requireValue(userId, "userId");
        AgentInvocationIdentity.requireValue(conversationId, "conversationId");
        AgentInvocationIdentity.requireValue(agentKey, "agentKey");

        return new AgentInvocationIdentity(namespace, userId, conversationId, agentKey,
                null, null, null);
    }
}
