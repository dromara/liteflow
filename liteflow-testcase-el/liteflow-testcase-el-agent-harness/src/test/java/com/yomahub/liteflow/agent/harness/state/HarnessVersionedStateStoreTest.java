package com.yomahub.liteflow.agent.harness.state;

import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class HarnessVersionedStateStoreTest {
    private static final String AGENT = "lf-" + "a".repeat(64);
    private static final String OTHER_AGENT = "lf-" + "b".repeat(64);
    private static final String SESSION = "lf-" + "c".repeat(64);

    @Test
    void casUsesTheSamePhysicalRoutesAsOrdinaryReadsIncludingChildAndSandboxState() {
        var backend = new InMemoryAgentStateStore();
        var first = new HarnessNamespacedAgentStateStore(backend, AGENT);
        var second = new HarnessNamespacedAgentStateStore(backend, OTHER_AGENT);
        assertTrue(first.supportsVersioning());
        for (String session : new String[]{SESSION, SESSION + "/child/research"}) {
            long version = first.saveIfVersion("alice", session, "agent_state", new UserMessage("first"), 0);
            assertTrue(version > 0);
            assertEquals(version, first.getVersioned("alice", session, "agent_state", UserMessage.class).version());
            assertEquals("first", first.get("alice", session, "agent_state", UserMessage.class)
                    .orElseThrow().getTextContent());
            assertFalse(second.getVersioned("alice", session, "agent_state", UserMessage.class).isPresent());
            assertFalse(first.getVersioned("bob", session, "agent_state", UserMessage.class).isPresent());
            first.save("alice", session, "agent_state", new UserMessage("newer"));
            assertEquals(AgentStateStore.UNVERSIONED,
                    first.saveIfVersion("alice", session, "agent_state", new UserMessage("stale"), version));
        }
        String sandboxSession = "sandbox/session/" + SESSION;
        long sandboxVersion = first.saveIfVersion(null, sandboxSession, "_sandbox_state", new UserMessage("snapshot"), 0);
        assertEquals(sandboxVersion,
                second.getVersioned(null, sandboxSession, "_sandbox_state", UserMessage.class).version());
        assertEquals("snapshot", second.get(null, sandboxSession, "_sandbox_state", UserMessage.class)
                .orElseThrow().getTextContent());
        assertThrows(IllegalArgumentException.class,
                () -> first.getVersioned("alice", sandboxSession, "_sandbox_state", UserMessage.class));
    }
}
