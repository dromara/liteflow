package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.property.agent.AgentConfig;
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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        CountDownLatch releaseSecond = new CountDownLatch(1);
        Thread first = queuedEntryThread(guard, key, 1, entered, firstEntered, releaseFirst);
        Thread second = queuedEntryThread(guard, key, 2, entered, secondEntered, releaseSecond);

        try (AgentInvocationLease holder = guard.acquire(key, WAIT)) {
            first.start();
            awaitThreadState(first, Thread.State.TIMED_WAITING);
            second.start();
            awaitThreadState(second, Thread.State.TIMED_WAITING);
            holder.close();
            assertTrue(firstEntered.await(2, TimeUnit.SECONDS));
            assertEquals(List.of(1), entered);
            awaitThreadState(second, Thread.State.TIMED_WAITING);
            releaseFirst.countDown();
            assertTrue(secondEntered.await(2, TimeUnit.SECONDS));
            releaseSecond.countDown();
        }
        first.join(2_000);
        second.join(2_000);
        assertFalse(first.isAlive());
        assertFalse(second.isAlive());

        assertEquals(List.of(1, 2), entered);
        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            assertEquals(key, ignored.key());
        }
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
            release.countDown();
            holder.get(2, TimeUnit.SECONDS);
        } catch (Exception exception) {
            throw new AssertionError(exception);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            assertEquals(key, ignored.key());
        }
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

        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            assertEquals(key, ignored.key());
        }
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
            awaitThreadState(waiter, Thread.State.TIMED_WAITING);
            waiter.interrupt();
            waiter.join(2_000);
            assertFalse(waiter.isAlive());
            assertEquals(AgentInvocationErrorType.INTERRUPTED, failure.get().getErrorType());
            assertTrue(interrupted.get());
        }
        try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
            assertEquals(key, ignored.key());
        }
    }

    @Test
    void veryLargePositiveTimeoutAcquiresAvailableKeyWithoutOverflow() {
        LocalAgentInvocationGuard guard = new LocalAgentInvocationGuard();

        assertDoesNotThrow(() -> {
            try (AgentInvocationLease ignored = guard.acquire(key("agent-a"), Duration.ofSeconds(Long.MAX_VALUE))) {
                assertTrue(ignored.fencingToken().isEmpty());
            }
        });
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
    void coordinatorClosesWorkspaceAfterStateCloseFailsAndSuppressesWorkspaceFailureOnce() {
        AgentInvocationKey workspace = AgentInvocationKey.workspace("tenant", "user", "conversation");
        AgentInvocationKey state = AgentInvocationKey.state("tenant", "user", "conversation", "agent");
        List<String> events = new ArrayList<>();
        RuntimeException stateFailure = new IllegalStateException("state close failed");
        RuntimeException workspaceFailure = new IllegalStateException("workspace close failed");
        AgentInvocationGuard guard = (key, timeout) -> new RecordingLease(key, events,
                key.scope() == AgentInvocationScope.STATE ? stateFailure : workspaceFailure);

        AgentInvocationLease lease = new AgentInvocationCoordinator(guard).acquire(workspace, state, WAIT);
        RuntimeException failure = assertThrows(RuntimeException.class, lease::close);

        assertEquals(stateFailure, failure);
        assertEquals(List.of("close:STATE", "close:WORKSPACE"), events);
        assertEquals(1, failure.getSuppressed().length);
        assertEquals(workspaceFailure, failure.getSuppressed()[0]);
    }

    @Test
    void localGuardIsSharedAcrossResolversForTheSameKey() throws Exception {
        AgentConfig config = new AgentConfig();
        config.getStateStore().setJsonRoot("target/agent-state");
        AgentInvocationGuard firstGuard = new AgentInvocationGuardResolver().resolve(config);
        AgentInvocationGuard secondGuard = new AgentInvocationGuardResolver().resolve(config);
        AgentInvocationKey key = key("agent-a");
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        Thread waiter = new Thread(() -> {
            try (AgentInvocationLease ignored = secondGuard.acquire(key, WAIT)) {
                entered.countDown();
                await(release);
            }
        });

        try (AgentInvocationLease ignored = firstGuard.acquire(key, WAIT)) {
            waiter.start();
            awaitThreadState(waiter, Thread.State.TIMED_WAITING);
            assertEquals(1, entered.getCount());
        }
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        release.countDown();
        waiter.join(2_000);
        assertFalse(waiter.isAlive());
    }

    private static Thread queuedEntryThread(LocalAgentInvocationGuard guard, AgentInvocationKey key, int value,
                                             List<Integer> entered, CountDownLatch enteredLatch,
                                             CountDownLatch release) {
        return new Thread(() -> {
            try (AgentInvocationLease ignored = guard.acquire(key, WAIT)) {
                synchronized (entered) {
                    entered.add(value);
                }
                enteredLatch.countDown();
                await(release);
            }
        });
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

    private static void awaitThreadState(Thread thread, Thread.State expected) {
        long deadline = System.nanoTime() + WAIT.toNanos();
        while (thread.getState() != expected && System.nanoTime() < deadline) {
            Thread.yield();
        }
        assertEquals(expected, thread.getState());
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

    private static final class RecordingLease implements AgentInvocationLease {
        private final AgentInvocationKey key;
        private final List<String> events;
        private final RuntimeException closeFailure;
        private final AtomicBoolean closed = new AtomicBoolean();

        private RecordingLease(AgentInvocationKey key, List<String> events) {
            this(key, events, null);
        }

        private RecordingLease(AgentInvocationKey key, List<String> events, RuntimeException closeFailure) {
            this.key = key;
            this.events = events;
            this.closeFailure = closeFailure;
        }

        @Override
        public AgentInvocationKey key() {
            return key;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                events.add("close:" + key.scope());
                if (closeFailure != null) {
                    throw closeFailure;
                }
            }
        }
    }
}
