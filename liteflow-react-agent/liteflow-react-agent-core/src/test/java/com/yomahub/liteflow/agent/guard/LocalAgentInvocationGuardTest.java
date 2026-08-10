package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalAgentInvocationGuardTest {

    private static final Duration WAIT = Duration.ofSeconds(2);

    @Test
    void serializesSameKeyInFifoOrderAndRemovesEntryAfterFinalLease() throws Exception {
        LocalAgentInvocationGuard guard = new LocalAgentInvocationGuard();
        AgentInvocationKey key = key("agent-a");
        List<Integer> entered = new ArrayList<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try (AgentInvocationLease holder = guard.acquire(key, WAIT)) {
            Future<?> first = executor.submit(() -> enter(guard, key, 1, entered));
            awaitWaiters(guard, key, 1);
            Future<?> second = executor.submit(() -> enter(guard, key, 2, entered));
            awaitWaiters(guard, key, 2);
            assertTrue(guard.isTracked(key));
            holder.close();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(List.of(1, 2), entered);
        assertFalse(guard.isTracked(key));
    }

    @Test
    void allowsDifferentKeysToEnterConcurrently() throws Exception {
        LocalAgentInvocationGuard guard = new LocalAgentInvocationGuard();
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> holdUntilReleased(guard, key("agent-a"), bothEntered, release));
            Future<?> second = executor.submit(() -> holdUntilReleased(guard, key("agent-b"), bothEntered, release));
            assertTrue(bothEntered.await(2, TimeUnit.SECONDS));
            release.countDown();
            first.get(2, TimeUnit.SECONDS);
            second.get(2, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void mapsAcquireTimeoutToTypedExceptionAndCleansTimedOutWaiter() {
        LocalAgentInvocationGuard guard = new LocalAgentInvocationGuard();
        AgentInvocationKey key = key("agent-a");
        CountDownLatch acquired = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> holder = executor.submit(() -> holdAfterAcquire(guard, key, acquired, release));
            await(acquired);
            AgentInvocationException exception = assertThrows(AgentInvocationException.class,
                    () -> guard.acquire(key, Duration.ofMillis(20)));
            assertEquals(AgentInvocationErrorType.TIMEOUT, exception.getErrorType());
            assertTrue(guard.isTracked(key));
            assertEquals(0, guard.waiterCount(key));
            release.countDown();
            holder.get(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
        assertFalse(guard.isTracked(key));
    }

    @Test
    void leaseIsIdempotentHasNoFencingTokenAndCleansUpAfterException() {
        LocalAgentInvocationGuard guard = new LocalAgentInvocationGuard();
        AgentInvocationKey key = key("agent-a");

        try {
            try (AgentInvocationLease lease = guard.acquire(key, WAIT)) {
                assertTrue(lease.fencingToken().isEmpty());
                lease.close();
                lease.close();
                throw new IllegalStateException("call failed");
            }
        } catch (IllegalStateException expected) {
            assertEquals("call failed", expected.getMessage());
        }

        assertFalse(guard.isTracked(key));
    }

    @Test
    void interruptionReleasesWaiterAndPreservesInterruptStatus() throws Exception {
        LocalAgentInvocationGuard guard = new LocalAgentInvocationGuard();
        AgentInvocationKey key = key("agent-a");
        AtomicReference<AgentInvocationException> failure = new AtomicReference<>();
        AtomicBoolean interrupted = new AtomicBoolean();
        Thread waiter = new Thread(() -> {
            try {
                guard.acquire(key, WAIT);
            } catch (AgentInvocationException exception) {
                failure.set(exception);
                interrupted.set(Thread.currentThread().isInterrupted());
            }
        });

        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            waiter.start();
            awaitWaiters(guard, key, 1);
            waiter.interrupt();
            waiter.join(2_000);
            assertFalse(waiter.isAlive());
            assertEquals(AgentInvocationErrorType.INTERRUPTED, failure.get().getErrorType());
            assertTrue(interrupted.get());
            assertEquals(0, guard.waiterCount(key));
        }
        assertFalse(guard.isTracked(key));
    }

    @Test
    void coordinatorAcquiresWorkspaceBeforeStateAndReleasesInReverseOrderOnFailure() {
        AgentInvocationKey workspace = AgentInvocationKey.workspace("tenant", "user", "conversation");
        AgentInvocationKey state = AgentInvocationKey.state("tenant", "user", "conversation", "agent");
        List<String> events = new ArrayList<>();
        AgentInvocationGuard guard = (key, timeout) -> {
            events.add("acquire:" + key.scope());
            if (key.equals(state)) {
                throw new AgentInvocationException(AgentInvocationErrorType.TIMEOUT, "state unavailable");
            }
            return new RecordingLease(key, events);
        };

        AgentInvocationCoordinator coordinator = new AgentInvocationCoordinator(guard);
        AgentInvocationException exception = assertThrows(AgentInvocationException.class,
                () -> coordinator.acquire(workspace, state, WAIT));

        assertEquals(AgentInvocationErrorType.TIMEOUT, exception.getErrorType());
        assertEquals(List.of("acquire:WORKSPACE", "acquire:STATE", "close:WORKSPACE"), events);
    }

    @Test
    void coordinatorReleasesStateBeforeWorkspaceAfterSuccessfulAcquisition() {
        AgentInvocationKey workspace = AgentInvocationKey.workspace("tenant", "user", "conversation");
        AgentInvocationKey state = AgentInvocationKey.state("tenant", "user", "conversation", "agent");
        List<String> events = new ArrayList<>();
        AgentInvocationGuard guard = (key, timeout) -> {
            events.add("acquire:" + key.scope());
            return new RecordingLease(key, events);
        };

        try (AgentInvocationLease ignored = new AgentInvocationCoordinator(guard).acquire(workspace, state, WAIT)) {
            assertEquals(List.of("acquire:WORKSPACE", "acquire:STATE"), events);
        }

        assertEquals(List.of("acquire:WORKSPACE", "acquire:STATE", "close:STATE", "close:WORKSPACE"), events);
    }

    @Test
    void resolverRejectsPotentiallyDistributedBeanStoreWithoutCoordinationInStrictMode() {
        AgentConfig config = distributedBeanStoreConfig();

        assertThrows(com.yomahub.liteflow.agent.exception.AgentConfigException.class,
                () -> new AgentInvocationGuardResolver().validate(config));
    }

    @Test
    void resolverWarnsAndAllowsLocalCoordinationForNonStrictBeanStore() {
        AgentConfig config = distributedBeanStoreConfig();
        config.getInvocationGuard().setStrictDistributed(false);
        List<String> warnings = new ArrayList<>();

        new AgentInvocationGuardResolver(warnings::add).validate(config);

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("strictDistributed=false"));
    }

    private static void enter(LocalAgentInvocationGuard guard, AgentInvocationKey key, int value, List<Integer> entered) {
        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            synchronized (entered) {
                entered.add(value);
            }
        }
    }

    private static void holdUntilReleased(LocalAgentInvocationGuard guard, AgentInvocationKey key,
                                          CountDownLatch bothEntered, CountDownLatch release) {
        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            bothEntered.countDown();
            await(release);
        }
    }

    private static void holdAfterAcquire(LocalAgentInvocationGuard guard, AgentInvocationKey key,
                                         CountDownLatch acquired, CountDownLatch release) {
        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            acquired.countDown();
            await(release);
        }
    }

    private static void awaitWaiters(LocalAgentInvocationGuard guard, AgentInvocationKey key, int expected)
            throws InterruptedException {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (guard.waiterCount(key) != expected && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertEquals(expected, guard.waiterCount(key));
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(2, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static AgentInvocationKey key(String agentKey) {
        AgentInvocationIdentity identity = new InvocationIdentityResolver("tenant")
                .resolve("user", "conversation", agentKey);
        return AgentInvocationKey.state(identity);
    }

    private static AgentConfig distributedBeanStoreConfig() {
        AgentConfig config = new AgentConfig();
        config.getStateStore().setType(AgentStateStoreType.BEAN);
        return config;
    }

    private static final class RecordingLease implements AgentInvocationLease {
        private final AgentInvocationKey key;
        private final List<String> events;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RecordingLease(AgentInvocationKey key, List<String> events) {
            this.key = key;
            this.events = events;
        }

        @Override
        public AgentInvocationKey key() {
            return key;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                events.add("close:" + key.scope());
            }
        }
    }
}
