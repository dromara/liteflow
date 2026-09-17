package com.yomahub.liteflow.agent.context;

import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InvocationIdentityResolverTest {
    @Test void retainsPlainConversationIdsAndIsolatesApplicationsAndAgents() {
        var base = new InvocationIdentityResolver("app-a").resolve("chat-123", "agent-a");
        var otherApp = new InvocationIdentityResolver("app-b").resolve("chat-123", "agent-a");
        var otherAgent = new InvocationIdentityResolver("app-a").resolve("chat-123", "agent-b");
        assertEquals("chat-123", base.runtimeSessionId());
        assertNotEquals(base.storeSessionId(), otherApp.storeSessionId());
        assertNotEquals(base.storeSessionId(), otherAgent.storeSessionId());
        assertNotEquals(AgentInvocationKey.workspace(base), AgentInvocationKey.workspace(otherApp));
        assertEquals(AgentInvocationKey.workspace(base), AgentInvocationKey.workspace(otherAgent));
        assertNotEquals(AgentInvocationKey.state(base), AgentInvocationKey.state(otherAgent));
    }

    @Test void rejectsDirectoryEscapesInsteadOfSilentlyHashingIds() {
        for (String unsafe : new String[]{"", " ", ".", "..", "../chat", "a/b", "a\\b", "a:b", "chat."}) {
            assertThrows(IllegalArgumentException.class,
                    () -> new InvocationIdentityResolver("app").resolve(unsafe, "agent"));
            assertThrows(IllegalArgumentException.class,
                    () -> new InvocationIdentityResolver(unsafe).resolve("chat", "agent"));
        }
        assertEquals("中文会话", new InvocationIdentityResolver("应用").resolve("中文会话", "agent").runtimeSessionId());
    }

    @Test void directConstructionRecomputesDerivedIdentifiers() {
        var identity = new AgentInvocationIdentity("app", "chat", "agent", "forged", "forged", "forged");
        assertEquals(new InvocationIdentityResolver("app").resolve("chat", "agent"), identity);
    }
}
