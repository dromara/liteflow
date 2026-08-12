package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
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
        delegate.save("alice", NAMESPACE + ".malformed", "single", second);
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
    void resolverCreatesOwnedBuiltInsAndBorrowsNamedBeans(@TempDir Path tempDir) {
        CloseTrackingStore beanStore = new CloseTrackingStore();
        DefaultAgentStateStoreResolver resolver = new DefaultAgentStateStoreResolver(
                name -> "sharedStore".equals(name) ? beanStore : null);

        AgentStateStoreConfig memoryConfig = new AgentStateStoreConfig();
        memoryConfig.setType(AgentStateStoreType.MEMORY);
        ResolvedAgentStateStore memory = resolver.resolve(memoryConfig);
        assertTrue(memory.owned());
        assertInstanceOf(InMemoryAgentStateStore.class, memory.store());

        AgentStateStoreConfig jsonConfig = new AgentStateStoreConfig();
        jsonConfig.setType(AgentStateStoreType.JSON);
        jsonConfig.setJsonRoot(tempDir.toString());
        ResolvedAgentStateStore json = resolver.resolve(jsonConfig);
        assertTrue(json.owned());
        assertEquals(tempDir, ((JsonFileAgentStateStore) json.store()).getRootDirectory());

        AgentStateStoreConfig beanConfig = new AgentStateStoreConfig();
        beanConfig.setType(AgentStateStoreType.BEAN);
        beanConfig.setBeanName("sharedStore");
        ResolvedAgentStateStore bean = resolver.resolve(beanConfig);
        assertFalse(bean.owned());
        assertSame(beanStore, bean.store());
        bean.close();
        assertEquals(0, beanStore.closeCount.get());

        memory.close();
        json.close();
    }

    @Test
    void resolverRejectsNullBlankMissingAndWrongStoreConfiguration() {
        DefaultAgentStateStoreResolver resolver = new DefaultAgentStateStoreResolver(name -> {
            if ("wrong".equals(name)) {
                return "not-a-store";
            }
            return null;
        });

        assertThrows(RuntimeException.class, () -> resolver.resolve(null));

        AgentStateStoreConfig nullType = new AgentStateStoreConfig();
        nullType.setType(null);
        assertThrows(RuntimeException.class, () -> resolver.resolve(nullType));

        AgentStateStoreConfig blankJson = new AgentStateStoreConfig();
        blankJson.setType(AgentStateStoreType.JSON);
        blankJson.setJsonRoot(" ");
        assertThrows(RuntimeException.class, () -> resolver.resolve(blankJson));

        AgentStateStoreConfig blankBean = new AgentStateStoreConfig();
        blankBean.setType(AgentStateStoreType.BEAN);
        blankBean.setBeanName(" ");
        assertThrows(RuntimeException.class, () -> resolver.resolve(blankBean));

        AgentStateStoreConfig missingBean = new AgentStateStoreConfig();
        missingBean.setType(AgentStateStoreType.BEAN);
        missingBean.setBeanName("missing");
        assertThrows(RuntimeException.class, () -> resolver.resolve(missingBean));

        AgentStateStoreConfig wrongBean = new AgentStateStoreConfig();
        wrongBean.setType(AgentStateStoreType.BEAN);
        wrongBean.setBeanName("wrong");
        assertThrows(RuntimeException.class, () -> resolver.resolve(wrongBean));

        RuntimeException lookupFailure = new RuntimeException("missing bean");
        DefaultAgentStateStoreResolver throwingResolver =
                new DefaultAgentStateStoreResolver(name -> { throw lookupFailure; });
        AgentConfigException thrown = assertThrows(AgentConfigException.class,
                () -> throwingResolver.resolve(missingBean));
        assertSame(lookupFailure, thrown.getCause());
    }

    @Test
    void resolverWrapsMalformedJsonRootAsConfigurationFailure() {
        DefaultAgentStateStoreResolver resolver =
                new DefaultAgentStateStoreResolver(name -> null);
        AgentStateStoreConfig malformedJson = new AgentStateStoreConfig();
        malformedJson.setType(AgentStateStoreType.JSON);
        malformedJson.setJsonRoot("invalid\0root");

        AgentConfigException thrown = assertThrows(AgentConfigException.class,
                () -> resolver.resolve(malformedJson));

        assertTrue(thrown.getMessage().contains("liteflow.agent.state-store.json-root"));
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
