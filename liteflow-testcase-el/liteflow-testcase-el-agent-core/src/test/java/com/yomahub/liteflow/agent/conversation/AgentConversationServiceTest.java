package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class AgentConversationServiceTest {
    @TempDir Path root;

    @Test
    void jsonHistorySurvivesReopenAndPagesWithoutReadingTheWholeTranscript() {
        AgentConfig config = config("app");
        config.getStateStore().setJsonRoot(root.toString());
        String id;
        try (var service = AgentConversationService.open(config)) {
            id = service.create("alice", "标题").id();
            for (int index = 0; index < 5; index++) {
                service.append("alice", id, "user", "input", "消息 " + index);
            }
            service.update("alice", id, "新标题", Map.of("label", "中文"));
        }
        try (var service = AgentConversationService.open(config)) {
            AgentConversation conversation = service.get("alice", id).orElseThrow();
            assertEquals("新标题", conversation.title());
            assertEquals(5, conversation.messageCount());
            assertEquals("中文", conversation.attributes().get("label"));
            var first = service.messages("alice", id, 0, 2);
            assertEquals(List.of("消息 0", "消息 1"), first.items().stream().map(AgentConversationMessage::content).toList());
            var second = service.messages("alice", id, first.nextCursor(), 2);
            assertTrue(second.hasMore());
            var last = service.messages("alice", id, second.nextCursor(), 2);
            assertEquals(List.of(5L), last.items().stream().map(AgentConversationMessage::sequence).toList());
            assertFalse(last.hasMore());
            service.delete("alice", id);
        }
        try (var service = AgentConversationService.open(config)) {
            assertTrue(service.get("alice", id).isEmpty());
            assertTrue(service.list("alice", 0, 10).items().isEmpty());
            assertThrows(IllegalStateException.class, () -> service.create("alice", id, "again", true));
        }
    }

    @Test
    void listReadsOnlyMetadataAndMessagePagingReadsOnlyRequestedEntries() {
        class CountingStore extends InMemoryAgentStateStore {
            int messageReads;
            @Override public <T extends State> Optional<T> get(String user, String session, String key, Class<T> type) {
                if (key.startsWith("message_")) messageReads++;
                return super.get(user, session, key, type);
            }
        }
        CountingStore store = new CountingStore();
        try (var service = new AgentConversationService(config("app"), store)) {
            String id = service.create("alice", "test").id();
            for (int i = 0; i < 20; i++) service.append("alice", id, "user", "input", "message");
            assertEquals(20, service.list("alice", 0, 10).items().get(0).messageCount());
            assertEquals(0, store.messageReads);
            service.messages("alice", id, 10, 3);
            assertEquals(3, store.messageReads);
        }
    }

    @Test
    void isolatesNamespacesUsersAndUnsafeBusinessIdsAndDoesNotOwnBorrowedStores() {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        var first = new AgentConversationService(config("app-a"), store);
        var second = new AgentConversationService(config("app-b"), store);
        first.create("alice", "../../会话", "a", true);
        first.create("bob", "../../会话", "b", true);
        second.create("alice", "../../会话", "c", true);
        first.append("alice", "../../会话", "user", "input", "only alice");
        assertEquals(0, first.get("bob", "../../会话").orElseThrow().messageCount());
        assertEquals(0, second.get("alice", "../../会话").orElseThrow().messageCount());
        assertEquals(1, first.list("alice", 0, 10).items().size());
        first.close();
        assertThrows(IllegalStateException.class, () -> first.list("alice", 0, 10));
        assertTrue(second.get("alice", "../../会话").isPresent());
        second.close();
    }

    @Test
    void readsOldAgentStateWithoutCreatingHistoryAndReturnsADetachedSnapshot() {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        var identity = new InvocationIdentityResolver("app").resolve("alice", "old", "agent");
        store.save("alice", identity.storeSessionId(), "agent_state", AgentState.builder()
                .userId("alice").sessionId(identity.runtimeSessionId()).addMessage(new UserMessage("old input")).build());
        try (var service = new AgentConversationService(config("app"), store)) {
            AgentState snapshot = service.agentState("alice", "old", "agent").orElseThrow();
            assertEquals("old input", snapshot.getContext().get(0).getTextContent());
            snapshot.contextMutable().clear();
            assertEquals(1, service.agentState("alice", "old", "agent").orElseThrow().getContext().size());
            assertTrue(service.agentState("bob", "old", "agent").isEmpty());
            assertTrue(service.agentState("alice", "old", "other").isEmpty());
            assertTrue(service.list("alice", 0, 20).items().isEmpty());
        }
    }

    @Test
    void displayHistoryIsIndependentOfCompactionAndManualConversationsAvoidDuplicateMessages() {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        var identity = new InvocationIdentityResolver("app").resolve("alice", "auto", "agent");
        try (var service = new AgentConversationService(config("app"), store)) {
            service.beginInvocation(identity, identity.storeSessionId(), List.of(new UserMessage("original")), "request-1");
            service.finishInvocation(identity, "request-1", new UserMessage("answer"), null);
            store.save("alice", identity.storeSessionId(), "agent_state", AgentState.builder()
                    .addMessage(new UserMessage("compacted summary")).build());
            assertEquals(List.of("original", "answer"), service.messages("alice", "auto", 0, 10).items().stream()
                    .map(AgentConversationMessage::content).toList());
            assertEquals("agent", service.messages("alice", "auto", 0, 10).items().get(0).agentKey());
            var manual = new InvocationIdentityResolver("app").resolve("alice", "manual", "agent");
            service.create("alice", "manual", "web", false);
            service.append("alice", "manual", "user", "input", "UI input");
            service.beginInvocation(manual, manual.storeSessionId(), List.of(new UserMessage("transformed prompt")), "r");
            service.finishInvocation(manual, "r", new UserMessage("internal output"), null);
            assertEquals(1, service.get("alice", "manual").orElseThrow().messageCount());
        }
    }

    @Test
    void concurrentServiceInstancesAppendWithoutLosingMessages() throws Exception {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        try (var a = new AgentConversationService(config("app"), store);
             var b = new AgentConversationService(config("app"), store)) {
            a.create("alice", "shared", "test", true);
            var pool = Executors.newFixedThreadPool(4);
            try {
                var futures = java.util.stream.IntStream.range(0, 40).mapToObj(index -> pool.submit(() ->
                        (index % 2 == 0 ? a : b).append("alice", "shared", "user", "input", "" + index))).toList();
                for (var future : futures) future.get(5, TimeUnit.SECONDS);
                var messages = a.messages("alice", "shared", 0, 100).items();
                assertEquals(40, messages.size());
                assertEquals(40, messages.stream().map(AgentConversationMessage::content).distinct().count());
            } finally {
                pool.shutdownNow();
            }
        }
    }

    @Test
    void deletionWaitsForRunningAgentAndCannotBeRevivedByItsLateResult() throws Exception {
        AgentConfig config = config("app");
        CountDownLatch tombstoneWritten = new CountDownLatch(1);
        InMemoryAgentStateStore store = new InMemoryAgentStateStore() {
            @Override public void save(String user, String session, String key, State value) {
                super.save(user, session, key, value);
                if (value instanceof AgentConversationService.StoredConversation state && state.deleted()) {
                    tombstoneWritten.countDown();
                }
            }
        };
        var identity = new InvocationIdentityResolver("app").resolve("alice", "active", "agent");
        try (var service = new AgentConversationService(config, store)) {
            service.beginInvocation(identity, identity.storeSessionId(), List.of(new UserMessage("input")), "r");
            var guard = new AgentInvocationGuardResolver().resolve(config);
            var pool = Executors.newSingleThreadExecutor();
            try {
                java.util.concurrent.Future<?> deletion;
                try (var lease = guard.acquire(AgentInvocationKey.state(identity), Duration.ofSeconds(5))) {
                    deletion = pool.submit(() -> service.delete("alice", "active"));
                    assertTrue(tombstoneWritten.await(5, TimeUnit.SECONDS));
                    assertFalse(deletion.isDone());
                    store.save("alice", identity.storeSessionId(), "agent_state", AgentState.builder().build());
                    service.finishInvocation(identity, "r", new UserMessage("late answer"), null);
                }
                deletion.get(5, TimeUnit.SECONDS);
                assertFalse(store.exists("alice", identity.storeSessionId()));
                assertTrue(service.get("alice", "active").isEmpty());
                assertThrows(IllegalStateException.class, () -> service.beginInvocation(identity,
                        identity.storeSessionId(), List.of(new UserMessage("revive")), "r2"));
                service.delete("alice", "active");
            } finally {
                pool.shutdownNow();
            }
        }
    }

    @Test
    void failedMetadataWriteDoesNotPublishAPartialMessageAndImportIsRetryable() {
        class FailingStore extends InMemoryAgentStateStore {
            boolean fail;
            @Override public void save(String user, String session, String key, State value) {
                if (fail && "conversation".equals(key)) throw new IllegalStateException("metadata failure");
                super.save(user, session, key, value);
            }
        }
        FailingStore store = new FailingStore();
        try (var service = new AgentConversationService(config("app"), store)) {
            service.create("alice", "c", "title", false);
            store.fail = true;
            assertThrows(IllegalStateException.class, () -> service.append("alice", "c", "user", "input", "failed"));
            assertEquals(0, service.get("alice", "c").orElseThrow().messageCount());
            store.fail = false;
            service.append("alice", "c", "user", "input", "retry");
            assertEquals(List.of("retry"), service.messages("alice", "c", 0, 10).items().stream()
                    .map(AgentConversationMessage::content).toList());
            AgentConversation old = new AgentConversation("legacy", "old", 12, 34, 1, Map.of());
            var messages = List.of(new AgentConversationMessage(0, "original-id", "user", "input", "history", null, null, 15));
            store.fail = true;
            assertThrows(IllegalStateException.class, () -> service.importIfAbsent("alice", old, messages));
            store.fail = false;
            assertTrue(service.importIfAbsent("alice", old, messages));
            assertFalse(service.importIfAbsent("alice", old, messages));
            assertEquals("original-id", service.messages("alice", "legacy", 0, 1).items().get(0).id());
            service.delete("alice", "legacy");
            assertFalse(service.importIfAbsent("alice", old, messages));
        }
    }

    @Test
    void validatesBoundedPagingAndUnknownConversations() {
        try (var service = new AgentConversationService(config("app"), new InMemoryAgentStateStore())) {
            assertThrows(IllegalArgumentException.class, () -> service.list("alice", -1, 10));
            assertThrows(IllegalArgumentException.class, () -> service.list("alice", 0, 201));
            assertThrows(IllegalArgumentException.class, () -> service.messages("alice", "missing", 0, 10));
            assertThrows(IllegalArgumentException.class, () -> service.append("alice", "missing", "user", "input", "text"));
            assertTrue(service.list("alice", Long.MAX_VALUE, 10).items().isEmpty());
        }
    }

    private static AgentConfig config(String namespace) {
        AgentConfig config = new AgentConfig();
        config.getRuntime().setNamespace(namespace);
        return config;
    }
}
