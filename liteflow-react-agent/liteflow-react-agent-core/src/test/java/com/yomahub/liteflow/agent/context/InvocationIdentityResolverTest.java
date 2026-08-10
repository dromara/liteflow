package com.yomahub.liteflow.agent.context;

import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InvocationIdentityResolverTest {

    @Test
    void lengthPrefixedHashingKeepsPreviouslyAmbiguousValuesDistinct() {
        InvocationIdentityResolver resolver = new InvocationIdentityResolver("tenant-a");

        AgentInvocationIdentity slash = resolver.resolve("a/b", "conversation", "agent");
        AgentInvocationIdentity escaped = resolver.resolve("a_2Fb", "conversation", "agent");

        assertNotEquals(slash.runtimeSessionId(), escaped.runtimeSessionId());
        assertTrue(slash.runtimeSessionId().matches("lf-[0-9a-f]{64}"));
        assertTrue(slash.agentNamespace().matches("lf-[0-9a-f]{64}"));
        assertEquals(slash.agentNamespace() + "." + slash.runtimeSessionId(), slash.storeSessionId());
    }

    @Test
    void isolatesEachIdentityDimensionAndUsesAgentNamespaceOnlyForState() {
        InvocationIdentityResolver resolver = new InvocationIdentityResolver("tenant-a");
        AgentInvocationIdentity base = resolver.resolve("user-a", "conversation-a", "agent-a");
        AgentInvocationIdentity differentUser = resolver.resolve("user-b", "conversation-a", "agent-a");
        AgentInvocationIdentity differentConversation = resolver.resolve("user-a", "conversation-b", "agent-a");
        AgentInvocationIdentity differentAgent = resolver.resolve("user-a", "conversation-a", "agent-b");

        assertNotEquals(base.runtimeSessionId(), differentUser.runtimeSessionId());
        assertNotEquals(base.runtimeSessionId(), differentConversation.runtimeSessionId());
        assertNotEquals(base.agentNamespace(), differentAgent.agentNamespace());
        assertNotEquals(base.storeSessionId(), differentAgent.storeSessionId());

        AgentInvocationKey workspace = AgentInvocationKey.workspace(base);
        AgentInvocationKey state = AgentInvocationKey.state(base);
        assertNull(workspace.agentKey());
        assertEquals("agent-a", state.agentKey());
        assertEquals(workspace, AgentInvocationKey.workspace(differentAgent));
        assertNotEquals(state, AgentInvocationKey.state(differentAgent));
    }

    @Test
    void rejectsBlankIdentityPartsBeforeHashingOrKeyConstruction() {
        assertThrows(IllegalArgumentException.class, () -> new InvocationIdentityResolver(" "));

        InvocationIdentityResolver resolver = new InvocationIdentityResolver("tenant-a");
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve(null, "conversation", "agent"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("user", " ", "agent"));
        assertThrows(IllegalArgumentException.class, () -> resolver.resolve("user", "conversation", ""));
        assertDoesNotThrow(() -> AgentInvocationKey.workspace("tenant", "user", "conversation"));
        assertThrows(IllegalArgumentException.class,
                () -> AgentInvocationKey.state("tenant", "user", "conversation", " "));
    }

    @Test
    void directConstructionRecomputesDerivedIdentifiersInsteadOfAcceptingInconsistentValues() {
        AgentInvocationIdentity identity = new AgentInvocationIdentity(
                "tenant-a", "user-a", "conversation-a", "agent-a",
                "forged-runtime", "forged-agent-namespace", "forged-store-session");
        AgentInvocationIdentity resolved = new InvocationIdentityResolver("tenant-a")
                .resolve("user-a", "conversation-a", "agent-a");

        assertEquals(resolved.runtimeSessionId(), identity.runtimeSessionId());
        assertEquals(resolved.agentNamespace(), identity.agentNamespace());
        assertEquals(resolved.storeSessionId(), identity.storeSessionId());
    }
}
