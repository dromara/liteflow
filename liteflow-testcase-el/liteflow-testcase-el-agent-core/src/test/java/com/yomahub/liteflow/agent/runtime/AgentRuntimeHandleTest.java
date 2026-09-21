package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.exception.AgentException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRuntimeHandleTest {

    @Test
    void concurrentGetOrCreateBuildsAndPublishesExactlyOnce() throws Exception {
        AgentRuntimeHandle<CloseTrackingRuntime> handle = new AgentRuntimeHandle<>();
        AtomicInteger builds = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(8);
        try {
            List<Future<CloseTrackingRuntime>> futures = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                futures.add(executor.submit(() -> {
                    assertTrue(start.await(5, TimeUnit.SECONDS));
                    return handle.getOrCreate(() -> {
                        builds.incrementAndGet();
                        return new CloseTrackingRuntime();
                    });
                }));
            }
            start.countDown();
            CloseTrackingRuntime expected = futures.get(0).get(5, TimeUnit.SECONDS);
            for (Future<CloseTrackingRuntime> future : futures) {
                assertSame(expected, future.get(5, TimeUnit.SECONDS));
            }
        } finally {
            executor.shutdownNow();
            handle.close();
        }

        assertEquals(1, builds.get());
        assertTrue(handle.isInitialized());
        assertTrue(handle.isClosed());
    }

    @Test
    void failedCreationIsNotPublishedAndCanBeRetried() {
        AgentRuntimeHandle<CloseTrackingRuntime> handle = new AgentRuntimeHandle<>();
        RuntimeException creationFailure = new RuntimeException("build failed");

        assertSame(creationFailure, assertThrows(RuntimeException.class,
                () -> handle.getOrCreate(() -> { throw creationFailure; })));
        assertFalse(handle.isInitialized());

        CloseTrackingRuntime runtime = new CloseTrackingRuntime();
        assertSame(runtime, handle.getOrCreate(() -> runtime));
        assertTrue(handle.isInitialized());
        handle.close();
    }

    @Test
    void closeIsIdempotentAndClosedHandleRejectsNewBuilds() {
        AgentRuntimeHandle<CloseTrackingRuntime> handle = new AgentRuntimeHandle<>();
        CloseTrackingRuntime runtime = handle.getOrCreate(CloseTrackingRuntime::new);

        handle.close();
        assertDoesNotThrow(handle::close);

        assertEquals(1, runtime.closeCount.get());
        assertTrue(handle.isClosed());
        assertThrows(IllegalStateException.class,
                () -> handle.getOrCreate(CloseTrackingRuntime::new));
    }

    @Test
    void creationRacingCloseDoesNotLeakTheBuiltRuntime() throws Exception {
        AgentRuntimeHandle<CloseTrackingRuntime> handle = new AgentRuntimeHandle<>();
        CountDownLatch factoryEntered = new CountDownLatch(1);
        CountDownLatch allowFactoryReturn = new CountDownLatch(1);
        CountDownLatch closeStarted = new CountDownLatch(1);
        CloseTrackingRuntime runtime = new CloseTrackingRuntime();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> creating = executor.submit(() -> handle.getOrCreate(() -> {
                factoryEntered.countDown();
                await(allowFactoryReturn);
                return runtime;
            }));
            assertTrue(factoryEntered.await(5, TimeUnit.SECONDS));
            Future<?> closing = executor.submit(() -> {
                closeStarted.countDown();
                handle.close();
            });
            assertTrue(closeStarted.await(5, TimeUnit.SECONDS));
            allowFactoryReturn.countDown();
            creating.get(5, TimeUnit.SECONDS);
            closing.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertEquals(1, runtime.closeCount.get());
        assertTrue(handle.isClosed());
    }

    @Test
    void closeFailureIsPreservedAsAgentExceptionAndNotRetried() {
        RuntimeException closeFailure = new RuntimeException("runtime close failed");
        AgentRuntimeHandle<CloseTrackingRuntime> handle = new AgentRuntimeHandle<>();
        CloseTrackingRuntime runtime = new CloseTrackingRuntime(closeFailure);
        handle.getOrCreate(() -> runtime);

        AgentException thrown = assertThrows(AgentException.class, handle::close);
        assertSame(closeFailure, thrown.getCause());
        assertEquals(1, runtime.closeCount.get());
        assertDoesNotThrow(handle::close);
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AssertionError(exception);
        }
    }

    private static final class CloseTrackingRuntime implements AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger();
        private final RuntimeException closeFailure;

        private CloseTrackingRuntime() {
            this(null);
        }

        private CloseTrackingRuntime(RuntimeException closeFailure) {
            this.closeFailure = closeFailure;
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }
}
