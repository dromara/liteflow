package com.yomahub.liteflow.agent.redis;

import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.extensions.redis.state.RedisClientAdapter;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** Uses the official Redis store and an in-process transport to verify keys, JSON and deletion. */
class RedisConversationServiceTest {
    @Test
    void metadataPagingMessagesAndTombstonesRoundTripThroughRedisStore() {
        MemoryAdapter client = new MemoryAdapter();
        AgentConfig config = new AgentConfig();
        config.getRuntime().setNamespace("redis-conversation-test");
        String id;
        try (var service = new AgentConversationService(config,
                RedisAgentStateStore.builder().clientAdapter(client).build())) {
            id = service.create("alice", "Redis 会话").id();
            service.append("alice", id, "user", "input", "第一条");
            service.append("alice", id, "assistant", "result", "第二条");
        }
        try (var reopened = new AgentConversationService(config,
                RedisAgentStateStore.builder().clientAdapter(client).build())) {
            assertEquals("Redis 会话", reopened.list("alice", 0, 10).items().get(0).title());
            assertEquals("第二条", reopened.messages("alice", id, 1, 1).items().get(0).content());
            assertTrue(reopened.list("bob", 0, 10).items().isEmpty());
            reopened.delete("alice", id);
            assertTrue(reopened.list("alice", 0, 10).items().isEmpty());
            assertTrue(client.values.keySet().stream().noneMatch(key -> key.contains("message_")));
            assertThrows(IllegalStateException.class, () -> reopened.create("alice", id, "revive", true));
        }
    }

    private static final class MemoryAdapter implements RedisClientAdapter {
        final Map<String, String> values = new HashMap<>();
        final Map<String, Set<String>> sets = new HashMap<>();
        @Override public void set(String key, String value) { values.put(key, value); }
        @Override public String get(String key) { return values.get(key); }
        @Override public void rightPushList(String key, String value) { throw new AssertionError("Scalar message storage expected"); }
        @Override public List<String> rangeList(String key, long start, long end) { return List.of(); }
        @Override public long getListLength(String key) { return 0; }
        @Override public void deleteKeys(String... keys) {
            for (String key : keys) { values.remove(key); sets.remove(key); }
        }
        @Override public void addToSet(String key, String member) { sets.computeIfAbsent(key, ignored -> new HashSet<>()).add(member); }
        @Override public Set<String> getSetMembers(String key) { return Set.copyOf(sets.getOrDefault(key, Set.of())); }
        @Override public long getSetSize(String key) { return getSetMembers(key).size(); }
        @Override public boolean keyExists(String key) { return values.containsKey(key) || sets.containsKey(key); }
        @Override public Set<String> findKeysByPattern(String pattern) {
            String regex = java.util.Arrays.stream(pattern.split("\\*", -1)).map(Pattern::quote).collect(Collectors.joining(".*"));
            Set<String> keys = new HashSet<>(values.keySet());
            keys.addAll(sets.keySet());
            return keys.stream().filter(key -> key.matches(regex)).collect(Collectors.toSet());
        }
        @Override public void close() { }
    }
}
