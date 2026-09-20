package com.yomahub.liteflow.agent.context;

/** Generates collision-resistant AgentScope runtime and store identifiers. */
public final class InvocationIdentityResolver {

    private final String namespace;

    public InvocationIdentityResolver(String namespace) {
        AgentInvocationIdentity.requirePathSegment(namespace, "applicationName");
        this.namespace = namespace;
    }

    public AgentInvocationIdentity resolve(String conversationId, String agentKey) {
        AgentInvocationIdentity.requireValue(conversationId, "conversationId");
        AgentInvocationIdentity.requireValue(agentKey, "agentKey");

        return new AgentInvocationIdentity(namespace, conversationId, agentKey,
                null, null, null);
    }
}
