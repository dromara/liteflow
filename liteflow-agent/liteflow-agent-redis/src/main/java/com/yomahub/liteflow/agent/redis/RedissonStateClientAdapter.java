package com.yomahub.liteflow.agent.redis;

import io.agentscope.extensions.redis.state.RedisClientAdapter;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Adapts the project's Redisson 3 API; AgentScope 2.0.3's adapter requires newer APIs. */
final class RedissonStateClientAdapter implements RedisClientAdapter {
    private final RedissonClient client;

    RedissonStateClientAdapter(RedissonClient client) { this.client = client; }

    @Override public void set(String key, String value) {
        client.<String>getBucket(key, StringCodec.INSTANCE).set(value);
    }

    @Override public String get(String key) {
        return client.<String>getBucket(key, StringCodec.INSTANCE).get();
    }

    @Override public void rightPushList(String key, String value) {
        client.<String>getList(key, StringCodec.INSTANCE).add(value);
    }

    @Override public List<String> rangeList(String key, long start, long end) {
        return client.<String>getList(key, StringCodec.INSTANCE).range(Math.toIntExact(start), Math.toIntExact(end));
    }

    @Override public long getListLength(String key) {
        return client.getList(key, StringCodec.INSTANCE).size();
    }

    @Override public void deleteKeys(String... keys) { client.getKeys().delete(keys); }

    @Override public void addToSet(String key, String member) {
        client.<String>getSet(key, StringCodec.INSTANCE).add(member);
    }

    @Override public Set<String> getSetMembers(String key) {
        return client.<String>getSet(key, StringCodec.INSTANCE).readAll();
    }

    @Override public long getSetSize(String key) { return client.getSet(key, StringCodec.INSTANCE).size(); }

    @Override public boolean keyExists(String key) { return client.getKeys().countExists(key) > 0; }

    @Override public Set<String> findKeysByPattern(String pattern) {
        Set<String> keys = new HashSet<>();
        client.getKeys().getKeysByPattern(pattern).forEach(keys::add);
        return keys;
    }

    @Override public long evalScript(String script, List<String> keys, List<String> args) {
        Number result = client.getScript(StringCodec.INSTANCE).eval(RScript.Mode.READ_WRITE, script,
                RScript.ReturnType.INTEGER, new ArrayList<Object>(keys), args.toArray());
        return result.longValue();
    }

    @Override public void close() { client.shutdown(); }
}
