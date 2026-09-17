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
        config.setApplicationName("harness-history");
        config.getSessionStore().setJsonRoot(root.toString());
        var identities = new InvocationIdentityResolver("harness-history");
        var first = identities.resolve("conversation", "one");
        var second = identities.resolve("conversation", "two");
        var other = identities.resolve("other-conversation", "one");
        var raw = new JsonFileAgentStateStore(root);
        for (var identity : java.util.List.of(first, second, other)) {
            var wrapper = new HarnessNamespacedAgentStateStore(raw, identity.agentNamespace());
            wrapper.save(null, identity.runtimeSessionId(), "agent_state", AgentState.builder()
                    .userId(null).sessionId(identity.runtimeSessionId())
                    .addMessage(new UserMessage(identity.agentKey())).build());
        }
        try (var service = AgentConversationService.open(config)) {
            assertEquals("one", service.agentState("conversation", "one")
                    .orElseThrow().getContext().get(0).getTextContent());
            assertTrue(service.list(0, 20).items().isEmpty());
            service.create("conversation", "imported", false);
            service.attachAgent("conversation", "one");
            service.attachAgent("conversation", "two");
        }
        try (var reopened = AgentConversationService.open(config)) {
            reopened.delete("conversation");
            assertFalse(raw.exists(null, HarnessNamespacedAgentStateStore.physicalAgentSessionId(
                    first.agentNamespace(), first.runtimeSessionId())));
            assertFalse(raw.exists(null, HarnessNamespacedAgentStateStore.physicalAgentSessionId(
                    second.agentNamespace(), second.runtimeSessionId())));
            assertTrue(reopened.agentState("other-conversation", "one").isPresent());
        }
        raw.close();
    }
}
