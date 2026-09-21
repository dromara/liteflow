package com.yomahub.liteflow.agent.redis;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
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

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.NullSource
    @org.junit.jupiter.params.provider.ValueSource(longs = {-1, 0, 299})
    void distributedLeaseRejectsValuesBelowTheDocumentedMinimumBeforeConnecting(Long millis) {
        var config = new com.yomahub.liteflow.property.agent.AgentConfig();
        config.getInvocationGuard().setLeaseDuration(millis == null ? null : java.time.Duration.ofMillis(millis));
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new com.yomahub.liteflow.agent.redis.storage.RedisInvocationGuardProvider().resolve(config));
        assertTrue(failure.getMessage().contains("lease-duration"));
        assertTrue(failure.getMessage().contains("300ms"));
    }

    @Test
    void exactlyThreeHundredMillisecondsPassesLeaseValidationAndReachesConnectionValidation() {
        var config = new com.yomahub.liteflow.property.agent.AgentConfig();
        config.getInvocationGuard().setLeaseDuration(java.time.Duration.ofMillis(300));
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new com.yomahub.liteflow.agent.redis.storage.RedisInvocationGuardProvider().resolve(config));
        assertTrue(failure.getMessage().contains("requires uri or client-bean-name"));
    }

    private final RedisAgentStateStoreProvider provider =
            new RedisAgentStateStoreProvider(name -> {
                throw new IllegalStateException("no bean expected: " + name);
            });

    @Test
    void supportsRedisType() {
        assertEquals(AgentSessionStoreType.REDIS, provider.type());
    }

    @Test
    void rejectsMissingConnectionSource() {
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(new AgentSessionStoreConfig()));
        assertTrue(failure.getMessage().contains("uri"));
        assertTrue(failure.getMessage().contains("client-bean-name"));
    }

    @Test
    void rejectsUriAndBeanNameTogether() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getRedis().setUri("redis://localhost:6379");
        config.getRedis().setClientBeanName("redisClient");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> provider.resolve(config));
        assertTrue(failure.getMessage().contains("mutually exclusive"));
    }

    @Test
    void rejectsInvalidUri() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getRedis().setUri("not a uri");
        assertThrows(AgentConfigException.class, () -> provider.resolve(config));
    }

    @Test
    void uriModeWrapsMockEndpointFailureAsConfigException() throws Exception {
        try (var endpoint = new MockRedisEndpoint()) {
            AgentSessionStoreConfig config = new AgentSessionStoreConfig();
            config.getRedis().setUri(endpoint.uri());
            config.getRedis().setKeyPrefix("liteflow:agent:state:");
            AgentConfigException failure = assertThrows(AgentConfigException.class,
                    () -> provider.resolve(config));
            assertTrue(failure.getMessage().contains("Redis state store could not be created"));
            endpoint.assertRequested();
        }
    }

    @Test
    void beanModeAcceptsClientAdapterAndStaysUnowned() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getRedis().setClientBeanName("adapterBean");
        ResolvedAgentStateStore resolved = new RedisAgentStateStoreProvider(
                name -> new NoopRedisClientAdapter()).resolve(config);
        assertFalse(resolved.owned());
    }

    @Test
    void beanModeRejectsUnsupportedClientType() {
        AgentSessionStoreConfig config = new AgentSessionStoreConfig();
        config.getRedis().setClientBeanName("notAClient");
        AgentConfigException failure = assertThrows(AgentConfigException.class,
                () -> new RedisAgentStateStoreProvider(name -> "not a client").resolve(config));
        assertTrue(failure.getMessage().contains("UnifiedJedis"));
    }

    @Test
    void resolverDiscoversProviderThroughServiceLoader() throws Exception {
        try (var endpoint = new MockRedisEndpoint()) {
            AgentSessionStoreConfig config = new AgentSessionStoreConfig();
            config.setType(AgentSessionStoreType.REDIS);
            config.getRedis().setUri(endpoint.uri());
            AgentConfigException failure = assertThrows(AgentConfigException.class,
                    () -> new DefaultAgentStateStoreResolver().resolve(config));
            assertTrue(failure.getMessage().contains("Redis state store could not be created"));
            endpoint.assertRequested();
        }
    }

    /** A local RESP stub rejects the handshake; no assumption about an unused machine port. */
    private static final class MockRedisEndpoint implements AutoCloseable {
        private final java.net.ServerSocket server;
        private final java.util.concurrent.ExecutorService worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        private final java.util.concurrent.Future<?> request;

        MockRedisEndpoint() throws Exception {
            server = new java.net.ServerSocket(0, 1, java.net.InetAddress.getByName("127.0.0.1"));
            request = worker.submit(() -> {
                try (var socket = server.accept()) {
                    socket.setSoTimeout(3000);
                    assertTrue(socket.getInputStream().read() >= 0);
                    socket.getOutputStream().write("-ERR mock Redis handshake failure\r\n"
                            .getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    socket.getOutputStream().flush();
                }
                return null;
            });
        }
        String uri() { return "redis://127.0.0.1:" + server.getLocalPort(); }
        void assertRequested() throws Exception { request.get(5, java.util.concurrent.TimeUnit.SECONDS); }
        @Override public void close() throws Exception {
            try { server.close(); }
            finally { worker.shutdownNow(); }
        }
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
