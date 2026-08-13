package com.yomahub.liteflow.agent.tool;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ManagedProcessTreeTest {

    @Test
    void refreshUsesOneStableFrontierSnapshotWhenEveryGenerationCanGrow() throws Exception {
        AtomicBoolean grow = new AtomicBoolean(true);
        List<GrowingProcessHandle> generated = new CopyOnWriteArrayList<>();
        AtomicLong pidSequence = new AtomicLong(1000);
        GrowingProcessHandle parent = new GrowingProcessHandle(grow, generated, pidSequence);
        ManagedShellCommandTool.ManagedProcessTree tree =
                new ManagedShellCommandTool.ManagedProcessTree(parent);
        AtomicReference<Thread> worker = new AtomicReference<>();
        ExecutorService executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "managed-process-tree-refresh-test");
            worker.set(thread);
            return thread;
        });
        Future<?> refresh = executor.submit(tree::expandFrontier);
        boolean completedWithinBound = false;
        try {
            refresh.get(200, TimeUnit.MILLISECONDS);
            completedWithinBound = true;
        } catch (TimeoutException expectedForOldFixedPointLoop) {
            // The behavioral assertion below records the RED after bounded teardown.
        } finally {
            grow.set(false);
            refresh.get(2, TimeUnit.SECONDS);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(2, TimeUnit.SECONDS));
        }

        assertTrue(completedWithinBound,
                "a single frontier refresh must not chase newly discovered generations");
        assertEquals(1, parent.descendantsQueries.get());
        assertEquals(1, generated.size());
        assertEquals(0, generated.get(0).descendantsQueries.get(),
                "a handle discovered during refresh belongs to the next snapshot");
        assertFalse(worker.get().isAlive());
    }

    private static final class GrowingProcessHandle implements ProcessHandle {
        private final AtomicBoolean grow;
        private final List<GrowingProcessHandle> generated;
        private final AtomicLong pidSequence;
        private final long pid;
        private final AtomicBoolean alive = new AtomicBoolean(true);
        private final AtomicBoolean generatedChild = new AtomicBoolean();
        private final AtomicInteger descendantsQueries = new AtomicInteger();

        private GrowingProcessHandle(
                AtomicBoolean grow,
                List<GrowingProcessHandle> generated,
                AtomicLong pidSequence) {
            this.grow = grow;
            this.generated = generated;
            this.pidSequence = pidSequence;
            this.pid = pidSequence.incrementAndGet();
        }

        @Override
        public long pid() {
            return pid;
        }

        @Override
        public Optional<ProcessHandle> parent() {
            return Optional.empty();
        }

        @Override
        public Stream<ProcessHandle> children() {
            return Stream.empty();
        }

        @Override
        public Stream<ProcessHandle> descendants() {
            descendantsQueries.incrementAndGet();
            if (!grow.get() || !generatedChild.compareAndSet(false, true)) {
                return Stream.empty();
            }
            GrowingProcessHandle child =
                    new GrowingProcessHandle(grow, generated, pidSequence);
            generated.add(child);
            return Stream.of(child);
        }

        @Override
        public Info info() {
            return ProcessHandle.current().info();
        }

        @Override
        public CompletableFuture<ProcessHandle> onExit() {
            return CompletableFuture.completedFuture(this);
        }

        @Override
        public boolean supportsNormalTermination() {
            return true;
        }

        @Override
        public boolean destroy() {
            return alive.getAndSet(false);
        }

        @Override
        public boolean destroyForcibly() {
            return alive.getAndSet(false);
        }

        @Override
        public boolean isAlive() {
            return alive.get();
        }

        @Override
        public int compareTo(ProcessHandle other) {
            return Long.compare(pid, other.pid());
        }
    }
}
