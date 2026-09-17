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
        config.setApplicationName("redis-conversation-test");
        String id;
        try (var service = new AgentConversationService(config,
                RedisAgentStateStore.builder().clientAdapter(client).build())) {
            id = service.create("Redis 会话").id();
            service.append(id, "user", "input", "第一条");
            service.append(id, "assistant", "result", "第二条");
        }
        try (var reopened = new AgentConversationService(config,
                RedisAgentStateStore.builder().clientAdapter(client).build())) {
            assertEquals("Redis 会话", reopened.list(0, 10).items().get(0).title());
            assertEquals("第二条", reopened.messages(id, 1, 1).items().get(0).content());
            reopened.delete(id);
            assertTrue(reopened.list(0, 10).items().isEmpty());
            assertTrue(client.values.keySet().stream().noneMatch(key -> key.contains("message_")));
            assertThrows(IllegalStateException.class, () -> reopened.create(id, "revive", true));
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
        @Override public synchronized long evalScript(String script, List<String> keys, List<String> args) {
            assertEquals(io.agentscope.extensions.redis.state.RedisStateVersionSupport.SAVE_SCRIPT, script);
            long current = Long.parseLong(values.getOrDefault(keys.get(1), "0"));
            long expected = Long.parseLong(args.get(1));
            if (expected != -1 && expected != current) return -1;
            values.put(keys.get(0), args.get(0));
            values.put(keys.get(1), Long.toString(current + 1));
            addToSet(keys.get(2), args.get(2));
            return current + 1;
        }
        @Override public Set<String> findKeysByPattern(String pattern) {
            String regex = java.util.Arrays.stream(pattern.split("\\*", -1)).map(Pattern::quote).collect(Collectors.joining(".*"));
            Set<String> keys = new HashSet<>(values.keySet());
            keys.addAll(sets.keySet());
            return keys.stream().filter(key -> key.matches(regex)).collect(Collectors.toSet());
        }
        @Override public void close() { }
    }
}
