package com.yomahub.liteflow.agent.conversation;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.MessageMetadataKeys;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.util.JsonUtils;
import com.fasterxml.jackson.databind.JsonNode;
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
    void structuredRepliesWithoutTextRemainReadableAfterJsonStoreReopens() {
        AgentConfig config = config("structured-history");
        config.getSessionStore().setJsonRoot(root.toString());
        List<Object> payloads = List.of(Map.of("answer", 42), new StructuredAnswer(42),
                JsonUtils.getJsonCodec().fromJson("{\"answer\":42}", JsonNode.class));
        try (var service = AgentConversationService.open(config)) {
            for (int i = 0; i < payloads.size(); i++) {
                var identity = new InvocationIdentityResolver("structured-history").resolve("c" + i, "agent");
                service.beginInvocation(identity, identity.storeSessionId(), List.of(new UserMessage("question")), "r");
                Msg reply = Msg.builder().role(MsgRole.ASSISTANT)
                        .content(TextBlock.builder().text(i == 1 ? "  " : "").build())
                        .metadata(Map.of(MessageMetadataKeys.STRUCTURED_OUTPUT, payloads.get(i))).build();
                service.finishInvocation(identity, "r", reply, null);
            }
        }
        try (var service = AgentConversationService.open(config)) {
            for (int i = 0; i < payloads.size(); i++) {
                var message = service.messages("c" + i, 1, 1).items().get(0);
                assertFalse(message.content().isBlank(), "structured reply must not disappear from history");
                assertEquals(42, JsonUtils.getJsonCodec().fromJson(message.content(), JsonNode.class).get("answer").asInt());
                assertEquals("result", message.stage());
                assertEquals("agent", message.agentKey());
            }
        }
    }

    @Test
    void historyKeepsExistingDisplayTextAndDoesNotExposeUnrelatedMetadata() {
        try (var service = new AgentConversationService(config("app"), new InMemoryAgentStateStore())) {
            var identity = new InvocationIdentityResolver("app").resolve("text", "agent");
            service.beginInvocation(identity, identity.storeSessionId(), List.of(new UserMessage("question")), "r");
            service.finishInvocation(identity, "r", Msg.builder().role(MsgRole.ASSISTANT)
                    .content(TextBlock.builder().text("display answer").build())
                    .metadata(Map.of(MessageMetadataKeys.STRUCTURED_OUTPUT, Map.of("answer", 42))).build(), null);
            service.finishInvocation(identity, "r", Msg.builder().role(MsgRole.ASSISTANT)
                    .metadata(Map.of("internal_note", "not user content")).build(), null);
            service.finishInvocation(identity, "r", null, null);
            service.finishInvocation(identity, "r", Msg.builder().role(MsgRole.ASSISTANT)
                    .metadata(java.util.Collections.singletonMap(MessageMetadataKeys.STRUCTURED_OUTPUT, null)).build(), null);
            assertEquals(List.of("display answer", "", "", ""), service.messages("text", 1, 10)
                    .items().stream().map(AgentConversationMessage::content).toList());
        }
    }

    public record StructuredAnswer(int answer) { }

    @Test
    void readsUnversionedMetadataAndRejectsAnUnknownFutureFormat() {
        String legacy = """
                {"conversation":{"id":"legacy","title":"old","createdAt":1,"updatedAt":2,
                "messageCount":0,"attributes":{}},"recordAgentMessages":false,"agents":[],"deleted":false}
                """;
        var decoded = JsonUtils.getJsonCodec().fromJson(legacy, AgentConversationService.StoredConversation.class);
        assertEquals(1, decoded.schemaVersion());
        assertEquals("legacy", decoded.conversation().id());
        assertTrue(JsonUtils.getJsonCodec().toJson(decoded).contains("\"schemaVersion\":1"));
        String future = legacy.replace("\"deleted\":false", "\"deleted\":false,\"schemaVersion\":99");
        assertThrows(RuntimeException.class,
                () -> JsonUtils.getJsonCodec().fromJson(future, AgentConversationService.StoredConversation.class));
    }

    @Test
    void jsonHistorySurvivesReopenAndPagesWithoutReadingTheWholeTranscript() {
        AgentConfig config = config("app");
        config.getSessionStore().setJsonRoot(root.toString());
        String id;
        try (var service = AgentConversationService.open(config)) {
            id = service.create("标题").id();
            for (int index = 0; index < 5; index++) {
                service.append(id, "user", "input", "消息 " + index);
            }
            service.update(id, "新标题", Map.of("label", "中文"));
        }
        try (var service = AgentConversationService.open(config)) {
            AgentConversation conversation = service.get(id).orElseThrow();
            assertEquals("新标题", conversation.title());
            assertEquals(5, conversation.messageCount());
            assertEquals("中文", conversation.attributes().get("label"));
            var first = service.messages(id, 0, 2);
            assertEquals(List.of("消息 0", "消息 1"), first.items().stream().map(AgentConversationMessage::content).toList());
            var second = service.messages(id, first.nextCursor(), 2);
            assertTrue(second.hasMore());
            var last = service.messages(id, second.nextCursor(), 2);
            assertEquals(List.of(5L), last.items().stream().map(AgentConversationMessage::sequence).toList());
            assertFalse(last.hasMore());
            service.delete(id);
        }
        try (var service = AgentConversationService.open(config)) {
            assertTrue(service.get(id).isEmpty());
            assertTrue(service.list(0, 10).items().isEmpty());
            assertThrows(IllegalStateException.class, () -> service.create(id, "again", true));
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
            String id = service.create("test").id();
            for (int i = 0; i < 20; i++) service.append(id, "user", "input", "message");
            assertEquals(20, service.list(0, 10).items().get(0).messageCount());
            assertEquals(0, store.messageReads);
            service.messages(id, 10, 3);
            assertEquals(3, store.messageReads);
        }
    }

    @Test
    void isolatesApplicationsAndConversationsAndRejectsUnsafeIds() {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        var first = new AgentConversationService(config("app-a"), store);
        var second = new AgentConversationService(config("app-b"), store);
        first.create("chat", "a", true);
        first.create("other", "b", true);
        second.create("chat", "c", true);
        first.append("chat", "user", "input", "only app-a/chat");
        assertEquals(0, first.get("other").orElseThrow().messageCount());
        assertEquals(0, second.get("chat").orElseThrow().messageCount());
        assertEquals(2, first.list(0, 10).items().size());
        assertThrows(IllegalArgumentException.class, () -> first.create("../chat", "bad", true));
        first.close();
        assertThrows(IllegalStateException.class, () -> first.list(0, 10));
        assertTrue(second.get("chat").isPresent());
        second.close();
    }

    @Test
    void readsOldAgentStateWithoutCreatingHistoryAndReturnsADetachedSnapshot() {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        var identity = new InvocationIdentityResolver("app").resolve("old", "agent");
        store.save(null, identity.storeSessionId(), "agent_state", AgentState.builder()
                .userId(null).sessionId(identity.runtimeSessionId()).addMessage(new UserMessage("old input")).build());
        try (var service = new AgentConversationService(config("app"), store)) {
            AgentState snapshot = service.agentState("old", "agent").orElseThrow();
            assertEquals("old input", snapshot.getContext().get(0).getTextContent());
            snapshot.contextMutable().clear();
            assertEquals(1, service.agentState("old", "agent").orElseThrow().getContext().size());
            assertTrue(service.agentState("missing", "agent").isEmpty());
            assertTrue(service.agentState("old", "other").isEmpty());
            assertTrue(service.list(0, 20).items().isEmpty());
        }
    }

    @Test
    void displayHistoryIsIndependentOfCompactionAndManualConversationsAvoidDuplicateMessages() {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        var identity = new InvocationIdentityResolver("app").resolve("auto", "agent");
        try (var service = new AgentConversationService(config("app"), store)) {
            service.beginInvocation(identity, identity.storeSessionId(), List.of(new UserMessage("original")), "request-1");
            service.finishInvocation(identity, "request-1", new UserMessage("answer"), null);
            store.save(null, identity.storeSessionId(), "agent_state", AgentState.builder()
                    .addMessage(new UserMessage("compacted summary")).build());
            assertEquals(List.of("original", "answer"), service.messages("auto", 0, 10).items().stream()
                    .map(AgentConversationMessage::content).toList());
            assertEquals("agent", service.messages("auto", 0, 10).items().get(0).agentKey());
            var manual = new InvocationIdentityResolver("app").resolve("manual", "agent");
            service.create("manual", "web", false);
            service.append("manual", "user", "input", "UI input");
            service.beginInvocation(manual, manual.storeSessionId(), List.of(new UserMessage("transformed prompt")), "r");
            service.finishInvocation(manual, "r", new UserMessage("internal output"), null);
            assertEquals(1, service.get("manual").orElseThrow().messageCount());
        }
    }

    @Test
    void concurrentServiceInstancesAppendWithoutLosingMessages() throws Exception {
        InMemoryAgentStateStore store = new InMemoryAgentStateStore();
        try (var a = new AgentConversationService(config("app"), store);
             var b = new AgentConversationService(config("app"), store)) {
            a.create("shared", "test", true);
            var pool = Executors.newFixedThreadPool(4);
            try {
                var futures = java.util.stream.IntStream.range(0, 40).mapToObj(index -> pool.submit(() ->
                        (index % 2 == 0 ? a : b).append("shared", "user", "input", "" + index))).toList();
                for (var future : futures) future.get(5, TimeUnit.SECONDS);
                var messages = a.messages("shared", 0, 100).items();
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
        var identity = new InvocationIdentityResolver("app").resolve("active", "agent");
        try (var service = new AgentConversationService(config, store)) {
            service.beginInvocation(identity, identity.storeSessionId(), List.of(new UserMessage("input")), "r");
            var guard = new AgentInvocationGuardResolver().resolve(config);
            var pool = Executors.newSingleThreadExecutor();
            try {
                java.util.concurrent.Future<?> deletion;
                try (var lease = guard.acquire(AgentInvocationKey.state(identity), Duration.ofSeconds(5))) {
                    deletion = pool.submit(() -> service.delete("active"));
                    assertTrue(tombstoneWritten.await(5, TimeUnit.SECONDS));
                    assertFalse(deletion.isDone());
                    store.save(null, identity.storeSessionId(), "agent_state", AgentState.builder().build());
                    service.finishInvocation(identity, "r", new UserMessage("late answer"), null);
                }
                deletion.get(5, TimeUnit.SECONDS);
                assertFalse(store.exists("alice", identity.storeSessionId()));
                assertTrue(service.get("active").isEmpty());
                assertThrows(IllegalStateException.class, () -> service.beginInvocation(identity,
                        identity.storeSessionId(), List.of(new UserMessage("revive")), "r2"));
                service.delete("active");
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
            service.create("c", "title", false);
            store.fail = true;
            assertThrows(IllegalStateException.class, () -> service.append("c", "user", "input", "failed"));
            assertEquals(0, service.get("c").orElseThrow().messageCount());
            store.fail = false;
            service.append("c", "user", "input", "retry");
            assertEquals(List.of("retry"), service.messages("c", 0, 10).items().stream()
                    .map(AgentConversationMessage::content).toList());
            AgentConversation old = new AgentConversation("legacy", "old", 12, 34, 1, Map.of());
            var messages = List.of(new AgentConversationMessage(0, "original-id", "user", "input", "history", null, null, 15));
            store.fail = true;
            assertThrows(IllegalStateException.class, () -> service.importIfAbsent(old, messages));
            store.fail = false;
            assertTrue(service.importIfAbsent(old, messages));
            assertFalse(service.importIfAbsent(old, messages));
            assertEquals("original-id", service.messages("legacy", 0, 1).items().get(0).id());
            service.delete("legacy");
            assertFalse(service.importIfAbsent(old, messages));
        }
    }

    @Test
    void validatesBoundedPagingAndUnknownConversations() {
        try (var service = new AgentConversationService(config("app"), new InMemoryAgentStateStore())) {
            assertThrows(IllegalArgumentException.class, () -> service.list(-1, 10));
            assertThrows(IllegalArgumentException.class, () -> service.list(0, 201));
            assertThrows(IllegalArgumentException.class, () -> service.messages("missing", 0, 10));
            assertThrows(IllegalArgumentException.class, () -> service.append("missing", "user", "input", "text"));
            assertTrue(service.list(Long.MAX_VALUE, 10).items().isEmpty());
        }
    }

    private static AgentConfig config(String namespace) {
        AgentConfig config = new AgentConfig();
        config.setApplicationName(namespace);
        return config;
    }
}
