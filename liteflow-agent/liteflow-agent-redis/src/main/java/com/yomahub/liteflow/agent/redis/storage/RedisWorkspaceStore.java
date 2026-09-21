package com.yomahub.liteflow.agent.redis.storage;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.agentscope.harness.agent.filesystem.remote.store.BaseStore;
import io.agentscope.harness.agent.filesystem.remote.store.StoreItem;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Single-key Lua updates provide CAS with every supported Redis client, including Cluster. */
public final class RedisWorkspaceStore implements BaseStore {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String PUT = """
            local old = redis.call('get', KEYS[1])
            local version = 0
            if old then version = tonumber(string.match(old, '^(%d+)')) end
            if ARGV[1] ~= '-1' and version ~= tonumber(ARGV[1]) then return 0 end
            redis.call('set', KEYS[1], tostring(version + 1) .. '\\n' .. ARGV[2])
            return 1
            """;
    private final RedisStorageConnection connection;
    private final String prefix;
    public RedisWorkspaceStore(RedisStorageConnection connection, String prefix) {
        this.connection = Objects.requireNonNull(connection);
        this.prefix = Objects.requireNonNull(prefix);
    }
    private String namespace(List<String> namespace) { return prefix + encode(json(namespace)) + ":"; }
    private static String encode(String value) { return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8)); }
    private static String json(Object value) {
        try { return JSON.writeValueAsString(value); }
        catch (Exception failure) { throw new IllegalArgumentException("Cannot encode workspace record", failure); }
    }
    private StoreItem decode(String raw) {
        if (raw == null) return null;
        try {
            int separator = raw.indexOf('\n');
            long version = Long.parseLong(raw.substring(0, separator));
            Map<String, Object> item = JSON.readValue(raw.substring(separator + 1), new TypeReference<>() {});
            @SuppressWarnings("unchecked") Map<String, Object> value = (Map<String, Object>) item.get("value");
            return new StoreItem((String) item.get("key"), value, version);
        } catch (Exception failure) { throw new IllegalStateException("Corrupt workspace record", failure); }
    }
    @Override public StoreItem get(List<String> ns, String key) { return decode(connection.get.apply(namespace(ns) + encode(key))); }
    @Override public void put(List<String> ns, String key, Map<String, Object> value) { update(ns, key, value, -1); }
    @Override public boolean putIfVersion(List<String> ns, String key, Map<String, Object> value, long expected) {
        if (expected < 0) throw new IllegalArgumentException("expectedVersion must be nonnegative");
        return update(ns, key, value, expected);
    }
    private boolean update(List<String> ns, String key, Map<String, Object> value, long expected) {
        return connection.eval.run(PUT, List.of(namespace(ns) + encode(key)),
                List.of(Long.toString(expected), json(Map.of("key", key, "value", value)))) == 1;
    }
    @Override public List<StoreItem> search(List<String> ns, int limit, int offset) {
        if (limit < 0 || offset < 0) throw new IllegalArgumentException("Invalid pagination");
        return connection.keys.apply(namespace(ns) + "*").stream().sorted()
                .map(connection.get).map(this::decode).filter(Objects::nonNull)
                .skip(offset).limit(limit).toList();
    }
    @Override public void delete(List<String> ns, String key) { connection.delete.accept(namespace(ns) + encode(key)); }
}
