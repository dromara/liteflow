package com.yomahub.liteflow.agent.harness.state;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.JsonFileAgentStateStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class HarnessConversationServiceTest {
    @TempDir Path root;

    @Test
    void discoversOldHarnessStateWithoutRuntimeAndDeletesOnlyAttachedAgents() {
        AgentConfig config = new AgentConfig();
        config.getRuntime().setNamespace("harness-history");
        config.getStateStore().setJsonRoot(root.toString());
        var identities = new InvocationIdentityResolver("harness-history");
        var first = identities.resolve("alice", "conversation", "one");
        var second = identities.resolve("alice", "conversation", "two");
        var other = identities.resolve("bob", "conversation", "one");
        var raw = new JsonFileAgentStateStore(root);
        for (var identity : java.util.List.of(first, second, other)) {
            var wrapper = new HarnessNamespacedAgentStateStore(raw, identity.agentNamespace());
            wrapper.save(identity.userId(), identity.runtimeSessionId(), "agent_state", AgentState.builder()
                    .userId(identity.userId()).sessionId(identity.runtimeSessionId())
                    .addMessage(new UserMessage(identity.agentKey())).build());
        }
        try (var service = AgentConversationService.open(config)) {
            assertEquals("one", service.agentState("alice", "conversation", "one")
                    .orElseThrow().getContext().get(0).getTextContent());
            assertTrue(service.list("alice", 0, 20).items().isEmpty());
            service.create("alice", "conversation", "imported", false);
            service.attachAgent("alice", "conversation", "one");
            service.attachAgent("alice", "conversation", "two");
        }
        try (var reopened = AgentConversationService.open(config)) {
            reopened.delete("alice", "conversation");
            assertFalse(raw.exists("alice", HarnessNamespacedAgentStateStore.physicalAgentSessionId(
                    first.agentNamespace(), first.runtimeSessionId())));
            assertFalse(raw.exists("alice", HarnessNamespacedAgentStateStore.physicalAgentSessionId(
                    second.agentNamespace(), second.runtimeSessionId())));
            assertTrue(reopened.agentState("bob", "conversation", "one").isPresent());
        }
        raw.close();
    }
}
