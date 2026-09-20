package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.property.agent.AgentSessionStoreConfig;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.state.State;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuardedNamespacedAgentStateStoreTest {

    private static final String NAMESPACE = "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String SESSION_A = "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String SESSION_C = "lf-cccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccccc";
    private static final String PREFIXED_A = NAMESPACE + "." + SESSION_A;
    private static final String PREFIXED_C = NAMESPACE + "." + SESSION_C;

    @Test
    void versionedWritesRejectStaleUpdatesAndKeepUsersAndSessionsIsolated() {
        InMemoryAgentStateStore delegate = new InMemoryAgentStateStore();
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(delegate, NAMESPACE);
        assertTrue(store.supportsVersioning());
        assertEquals(0, store.getVersioned("alice", SESSION_A, "state", UserMessage.class).version());
        long first = store.saveIfVersion("alice", SESSION_A, "state", new UserMessage("one"), 0);
        var baseline = store.getVersioned("alice", SESSION_A, "state", UserMessage.class);
        assertEquals(first, baseline.version());
        long second = store.saveIfVersion("alice", SESSION_A, "state", new UserMessage("two"), first);
        assertTrue(second > first);
        assertEquals(AgentStateStore.UNVERSIONED,
                store.saveIfVersion("alice", SESSION_A, "state", new UserMessage("stale"), first));
        assertEquals("two", delegate.get("alice", PREFIXED_A, "state", UserMessage.class)
                .orElseThrow().getTextContent());
        assertFalse(store.getVersioned("bob", SESSION_A, "state", UserMessage.class).isPresent());
        assertFalse(store.getVersioned("alice", SESSION_C, "state", UserMessage.class).isPresent());
    }

    @Test
    void jsonBackendExplicitlyReportsUnversionedState(@TempDir Path root) {
        GuardedNamespacedAgentStateStore store = new GuardedNamespacedAgentStateStore(
                new JsonFileAgentStateStore(root), NAMESPACE);
        assertFalse(store.supportsVersioning());
        assertEquals(AgentStateStore.UNVERSIONED,
                store.saveIfVersion("alice", SESSION_A, "state", new UserMessage("one"), 0));
        assertEquals(AgentStateStore.UNVERSIONED,
                store.getVersioned("alice", SESSION_A, "state", UserMessage.class).version());
    }

    @Test
    void routesEverySessionOperationAndFiltersForeignOrMalformedListings() {
        CloseTrackingStore delegate = new CloseTrackingStore();
        GuardedNamespacedAgentStateStore store =
                new GuardedNamespacedAgentStateStore(delegate, NAMESPACE);
        UserMessage first = new UserMessage("one");
        UserMessage second = new UserMessage("two");

        store.save("alice", SESSION_A, "single", first);
        store.save("alice", SESSION_A, "list", List.of(first, second));

        assertEquals(first, store.get("alice", SESSION_A, "single", UserMessage.class).orElseThrow());
        assertEquals(List.of(first, second),
                store.getList("alice", SESSION_A, "list", UserMessage.class));
        assertTrue(store.exists("alice", SESSION_A));
        assertEquals(Set.of(PREFIXED_A), delegate.listSessionIds("alice"));

        delegate.save("alice", "lf-dddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddddd."
                + SESSION_C, "single", second);
        delegate.save("alice", NAMESPACE + ".../escape", "single", second);
        delegate.save("alice", PREFIXED_C, "single", second);
        assertEquals(Set.of(SESSION_A, SESSION_C), store.listSessionIds("alice"));

        store.delete("alice", SESSION_A, "single");
        assertTrue(store.get("alice", SESSION_A, "single", UserMessage.class).isEmpty());
        store.delete("alice", SESSION_A);
        assertFalse(delegate.exists("alice", PREFIXED_A));

        store.close();
        assertEquals(0, delegate.closeCount.get());
    }

    @Test
    void rejectsUnsafeNamespaceAndLogicalSessionIdsBeforeDelegation() {
        CloseTrackingStore delegate = new CloseTrackingStore();

        assertThrows(IllegalArgumentException.class,
                () -> new GuardedNamespacedAgentStateStore(delegate, "agent-a"));

        GuardedNamespacedAgentStateStore store =
                new GuardedNamespacedAgentStateStore(delegate, NAMESPACE);
        assertThrows(IllegalArgumentException.class,
                () -> store.exists("alice", "../../foreign"));
        assertTrue(delegate.listSessionIds("alice").isEmpty());
    }

    @Test
    void providerSubclassCanRouteThroughTheBorrowedDelegateSeam() {
        CloseTrackingStore delegate = new CloseTrackingStore();
        ProviderNamespacedStore store = new ProviderNamespacedStore(delegate, NAMESPACE);

        assertSame(delegate, store.borrowedDelegate());

        store.close();
        assertEquals(0, delegate.closeCount.get());
    }

    @Test
    void recordsEachConcurrentLogicalLoadFailureAndConsumesItOnce() throws Exception {
        RuntimeException failureA = new RuntimeException("load-a");
        RuntimeException failureC = new RuntimeException("load-c");
        FailingReadStore delegate = new FailingReadStore(failureA, failureC);
        GuardedNamespacedAgentStateStore store =
                new GuardedNamespacedAgentStateStore(delegate, NAMESPACE);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> {
                await(start);
                assertThrows(RuntimeException.class,
                        () -> store.get("alice", SESSION_A, "state", UserMessage.class));
            });
            Future<?> second = executor.submit(() -> {
                await(start);
                assertThrows(RuntimeException.class,
                        () -> store.getList("alice", SESSION_C, "state", UserMessage.class));
            });
            start.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertSame(failureA, store.takeLoadFailure("alice", SESSION_A).orElseThrow());
        assertTrue(store.takeLoadFailure("alice", SESSION_A).isEmpty());
        assertSame(failureC, store.takeLoadFailure("alice", SESSION_C).orElseThrow());
        assertDoesNotThrow(() -> store.clearLoadFailure("alice", SESSION_C));
        assertDoesNotThrow(() -> store.clearLoadFailure("alice", SESSION_C));
    }

    @Test
    void recordsExistsFailuresAgainstTheOriginalLogicalSession() {
        RuntimeException failure = new RuntimeException("exists-failed");
        FailingExistsStore delegate = new FailingExistsStore(failure);
        GuardedNamespacedAgentStateStore store =
                new GuardedNamespacedAgentStateStore(delegate, NAMESPACE);

        assertSame(failure, assertThrows(RuntimeException.class,
                () -> store.exists(null, SESSION_A)));
        assertSame(failure, store.takeLoadFailure(null, SESSION_A).orElseThrow());
    }

    @Test
    void resolverCreatesOwnedJsonStore(@TempDir Path tempDir) {
        DefaultAgentStateStoreResolver resolver = new DefaultAgentStateStoreResolver(List.of());

        AgentSessionStoreConfig jsonConfig = new AgentSessionStoreConfig();
        jsonConfig.setType(AgentSessionStoreType.JSON);
        jsonConfig.setJsonRoot(tempDir.toString());
        ResolvedAgentStateStore json = resolver.resolve(jsonConfig);
        assertTrue(json.owned());
        assertEquals(tempDir, ((JsonFileAgentStateStore) json.store()).getRootDirectory());

        json.close();
    }

    @Test
    void resolverRejectsNullBlankAndMissingProviderConfiguration() {
        DefaultAgentStateStoreResolver emptyResolver = new DefaultAgentStateStoreResolver(List.of());

        assertThrows(RuntimeException.class, () -> emptyResolver.resolve(null));

        AgentSessionStoreConfig nullType = new AgentSessionStoreConfig();
        nullType.setType(null);
        assertThrows(RuntimeException.class, () -> emptyResolver.resolve(nullType));

        AgentSessionStoreConfig blankJson = new AgentSessionStoreConfig();
        blankJson.setType(AgentSessionStoreType.JSON);
        blankJson.setJsonRoot(" ");
        assertThrows(RuntimeException.class, () -> emptyResolver.resolve(blankJson));

        AgentSessionStoreConfig redisConfig = new AgentSessionStoreConfig();
        redisConfig.setType(AgentSessionStoreType.REDIS);
        AgentConfigException redisFailure = assertThrows(AgentConfigException.class,
                () -> emptyResolver.resolve(redisConfig));
        assertTrue(redisFailure.getMessage().contains("liteflow-agent-redis"));

        AgentSessionStoreConfig mysqlConfig = new AgentSessionStoreConfig();
        mysqlConfig.setType(AgentSessionStoreType.MYSQL);
        AgentConfigException mysqlFailure = assertThrows(AgentConfigException.class,
                () -> emptyResolver.resolve(mysqlConfig));
        assertTrue(mysqlFailure.getMessage().contains("liteflow-agent-mysql"));
    }

    @Test
    void resolverDelegatesToDiscoveredProvidersAndValidatesTheirResult() {
        CloseTrackingStore providerStore = new CloseTrackingStore();
        AgentStateStoreProvider redisProvider = new AgentStateStoreProvider() {
            @Override
            public AgentSessionStoreType type() {
                return AgentSessionStoreType.REDIS;
            }

            @Override
            public ResolvedAgentStateStore resolve(AgentSessionStoreConfig config) {
                return "fail".equals(config.getRedis().getUri())
                        ? null
                        : new ResolvedAgentStateStore(providerStore, false);
            }
        };
        DefaultAgentStateStoreResolver resolver =
                new DefaultAgentStateStoreResolver(List.of(redisProvider));

        AgentSessionStoreConfig redisConfig = new AgentSessionStoreConfig();
        redisConfig.setType(AgentSessionStoreType.REDIS);
        ResolvedAgentStateStore redis = resolver.resolve(redisConfig);
        assertFalse(redis.owned());
        assertSame(providerStore, redis.store());
        redis.close();
        assertEquals(0, providerStore.closeCount.get());

        AgentSessionStoreConfig failingConfig = new AgentSessionStoreConfig();
        failingConfig.setType(AgentSessionStoreType.REDIS);
        failingConfig.getRedis().setUri("fail");
        assertThrows(AgentConfigException.class, () -> resolver.resolve(failingConfig));
    }

    @Test
    void resolverWrapsMalformedJsonRootAsConfigurationFailure() {
        DefaultAgentStateStoreResolver resolver =
                new DefaultAgentStateStoreResolver(List.of());
        AgentSessionStoreConfig malformedJson = new AgentSessionStoreConfig();
        malformedJson.setType(AgentSessionStoreType.JSON);
        malformedJson.setJsonRoot("invalid\0root");

        AgentConfigException thrown = assertThrows(AgentConfigException.class,
                () -> resolver.resolve(malformedJson));

        assertTrue(thrown.getMessage().contains("liteflow.agent.session-store.json-root"));
        assertInstanceOf(InvalidPathException.class, thrown.getCause());
    }

    @Test
    void ownedCloseIsIdempotentAndPreservesDelegateFailureAsAgentException() {
        RuntimeException closeFailure = new RuntimeException("close-failed");
        AgentStateStore delegate = new CloseFailingStore(closeFailure);
        ResolvedAgentStateStore resolved = new ResolvedAgentStateStore(delegate, true);

        AgentException thrown = assertThrows(AgentException.class, resolved::close);
        assertSame(closeFailure, thrown.getCause());
        assertDoesNotThrow(resolved::close);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static class CloseTrackingStore extends InMemoryAgentStateStore {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }

    private static final class ProviderNamespacedStore
            extends GuardedNamespacedAgentStateStore {

        private ProviderNamespacedStore(AgentStateStore delegate, String agentNamespace) {
            super(delegate, agentNamespace);
        }

        private AgentStateStore borrowedDelegate() {
            return delegate();
        }
    }

    private static final class FailingReadStore extends InMemoryAgentStateStore {
        private final RuntimeException failureA;
        private final RuntimeException failureC;

        private FailingReadStore(RuntimeException failureA, RuntimeException failureC) {
            this.failureA = failureA;
            this.failureC = failureC;
        }

        @Override
        public <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            throw failureA;
        }

        @Override
        public <T extends State> List<T> getList(
                String userId, String sessionId, String key, Class<T> itemType) {
            throw failureC;
        }
    }

    private static final class FailingExistsStore extends InMemoryAgentStateStore {
        private final RuntimeException failure;

        private FailingExistsStore(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public boolean exists(String userId, String sessionId) {
            throw failure;
        }
    }

    private static final class CloseFailingStore extends InMemoryAgentStateStore {
        private final RuntimeException failure;

        private CloseFailingStore(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public void close() {
            throw failure;
        }
    }
}
