package com.yomahub.liteflow.agent.redis;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import io.agentscope.extensions.redis.state.RedisClientAdapter;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The agentscope Redis extension connects eagerly while building the store, so these
 * tests assert configuration wiring and failure wrapping instead of requiring a live
 * Redis server.
 */
class RedisAgentStateStoreProviderTest {

    /** Nothing listens on port 1; connection attempts fail immediately. */
    private static final String CLOSED_PORT_URI = "redis://localhost:1";

    private final RedisAgentStateStoreProvider provider =
            new RedisAgentStateStoreProvider(name -> {
                throw new IllegalStateException("no bean expected: " + name);
            });

    @Test
    void supportsRedisType() {
        assertEquals(AgentStateStoreType.REDIS, provider.type());
    }

    @Test
    void rejectsMissingConnectionSource() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(new AgentStateStoreConfig()));
        assertTrue(failure.getMessage().contains("uri"));
        assertTrue(failure.getMessage().contains("client-bean-name"));
    }

    @Test
    void rejectsUriAndBeanNameTogether() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getRedis().setUri("redis://localhost:6379");
        config.getRedis().setClientBeanName("redisClient");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsInvalidUri() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getRedis().setUri("not a uri");
        assertThrows(AgentConfigException.class, () -> provider.resolve(config));
    }

    @Test
    void uriModeWrapsConnectionFailureAsConfigException() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getRedis().setUri(CLOSED_PORT_URI);
        config.getRedis().setKeyPrefix("liteflow:agent:state:");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("Redis state store could not be created"));
    }

    @Test
    void beanModeAcceptsClientAdapterAndStaysUnowned() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getRedis().setClientBeanName("adapterBean");
        ResolvedAgentStateStore resolved = new RedisAgentStateStoreProvider(
                name -> new NoopRedisClientAdapter()).resolve(config);
        assertFalse(resolved.owned());
    }

    @Test
    void beanModeRejectsUnsupportedClientType() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.getRedis().setClientBeanName("notAClient");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new RedisAgentStateStoreProvider(name -> "not a client").resolve(config));
        assertTrue(failure.getMessage().contains("UnifiedJedis"));
    }

    @Test
    void resolverDiscoversProviderThroughServiceLoader() {
        AgentStateStoreConfig config = new AgentStateStoreConfig();
        config.setType(AgentStateStoreType.REDIS);
        config.getRedis().setUri(CLOSED_PORT_URI);
        // SPI wiring proven: the error comes from the provider's connection attempt,
        // not the missing-module path.
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new DefaultAgentStateStoreResolver().resolve(config));
        assertTrue(failure.getMessage().contains("Redis state store could not be created"));
    }

    /** Adapter stub: the clientAdapter builder branch stores it without connecting. */
    private static final class NoopRedisClientAdapter implements RedisClientAdapter {
        @Override
        public long evalScript(String script, java.util.List<String> keys, java.util.List<String> args) {
            throw new AssertionError("Provider resolution must not execute Redis scripts");
        }

        @Override
        public void set(String key, String value) { }

        @Override
        public String get(String key) {
            return null;
        }

        @Override
        public void rightPushList(String key, String value) { }

        @Override
        public List<String> rangeList(String key, long start, long end) {
            return List.of();
        }

        @Override
        public long getListLength(String key) {
            return 0;
        }

        @Override
        public void deleteKeys(String... keys) { }

        @Override
        public void addToSet(String key, String member) { }

        @Override
        public Set<String> getSetMembers(String key) {
            return Set.of();
        }

        @Override
        public long getSetSize(String key) {
            return 0;
        }

        @Override
        public boolean keyExists(String key) {
            return false;
        }

        @Override
        public Set<String> findKeysByPattern(String pattern) {
            return Set.of();
        }

        @Override
        public void close() { }
    }
}
