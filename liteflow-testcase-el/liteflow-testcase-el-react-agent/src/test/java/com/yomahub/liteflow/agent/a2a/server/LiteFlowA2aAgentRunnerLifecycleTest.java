package com.yomahub.liteflow.agent.a2a.server;

import io.agentscope.core.a2a.server.executor.runner.AgentRequestOptions;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.message.Msg;
import org.junit.jupiter.api.Test;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LiteFlowA2aAgentRunnerLifecycleTest {

    @Test
    void everyTerminalSignalClosesExactlyOnceAndReleasesTheTaskId() {
        IllegalStateException streamFailure = new IllegalStateException("model failed");
        ConcurrentLinkedQueue<RuntimeProbe> planned = new ConcurrentLinkedQueue<>();
        RuntimeProbe completed = new RuntimeProbe(Flux.empty(), false);
        RuntimeProbe failed = new RuntimeProbe(Flux.error(streamFailure), false);
        RuntimeProbe cancelled = new RuntimeProbe(Flux.never(), false);
        planned.add(completed);
        planned.add(failed);
        planned.add(cancelled);
        ProbeFactory factory = new ProbeFactory(ignored -> planned.remove());
        LiteFlowA2aAgentRunner runner = runner(factory);
        AgentRequestOptions request = options("task-1", "session-1");

        StepVerifier.create(runner.stream(List.of(), request)).verifyComplete();
        StepVerifier.create(runner.stream(List.of(), request))
                .expectErrorMatches(failure -> failure == streamFailure)
                .verify();
        StepVerifier.create(runner.stream(List.of(), request)).thenCancel().verify();
        runner.stop("task-1");

        assertEquals(1, completed.closes.get());
        assertEquals(1, failed.closes.get());
        assertEquals(1, cancelled.closes.get());
        assertEquals(0, completed.interrupts.get());
        assertEquals(0, failed.interrupts.get());
        assertEquals(0, cancelled.interrupts.get());
    }

    @Test
    void stopInterruptsOnlyItsTaskAndSuppressesCloseFailure() {
        ConcurrentMap<String, RuntimeProbe> probes = new ConcurrentHashMap<>();
        ProbeFactory factory = new ProbeFactory(taskId -> {
            RuntimeProbe probe = new RuntimeProbe(Flux.never(), "task-a".equals(taskId));
            probes.put(taskId, probe);
            return probe;
        });
        LiteFlowA2aAgentRunner runner = runner(factory);
        Disposable taskA = runner.stream(
                List.of(), options("task-a", "session-a")).subscribe();
        Disposable taskB = runner.stream(
                List.of(), options("task-b", "session-b")).subscribe();

        runner.stop("task-a");
        runner.stop("task-a");
        taskA.dispose();

        assertEquals(1, probes.get("task-a").interrupts.get());
        assertEquals(1, probes.get("task-a").closes.get());
        assertEquals(0, probes.get("task-b").interrupts.get());
        assertEquals(0, probes.get("task-b").closes.get());

        runner.stop("task-b");
        taskB.dispose();
        assertEquals(1, probes.get("task-b").interrupts.get());
        assertEquals(1, probes.get("task-b").closes.get());
    }

    @Test
    void duplicateTaskRaceOpensExactlyOneRuntime() {
        ProbeFactory factory = new ProbeFactory(
                ignored -> new RuntimeProbe(Flux.never(), false));
        LiteFlowA2aAgentRunner runner = runner(factory);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        Function<String, Boolean> invoke = ignored -> {
            ready.countDown();
            await(start);
            try {
                runner.stream(List.of(), options("task-race", "session-race"));
                return true;
            }
            catch (IllegalStateException duplicate) {
                assertEquals(
                        "A2A task is already active: task-race", duplicate.getMessage());
                return false;
            }
        };
        CompletableFuture<Boolean> first = CompletableFuture.supplyAsync(
                () -> invoke.apply("first"));
        CompletableFuture<Boolean> second = CompletableFuture.supplyAsync(
                () -> invoke.apply("second"));
        await(ready);
        start.countDown();

        long successful = List.of(first.join(), second.join()).stream()
                .filter(Boolean::booleanValue)
                .count();

        assertEquals(1, successful);
        assertEquals(1, factory.opens.get());
        runner.stop("task-race");
    }

    @Test
    void stopBeforeRuntimeStartStillInterruptsAndClosesTheOpenedRuntime() {
        CountDownLatch openEntered = new CountDownLatch(1);
        CountDownLatch allowOpen = new CountDownLatch(1);
        RuntimeProbe probe = new RuntimeProbe(Flux.never(), false);
        ProbeFactory factory = new ProbeFactory(ignored -> {
            openEntered.countDown();
            await(allowOpen);
            return probe;
        });
        LiteFlowA2aAgentRunner runner = runner(factory);
        CompletableFuture<Flux<?>> invocation = CompletableFuture.supplyAsync(
                () -> runner.stream(
                        List.of(), options("task-stopping", "session-stopping")));
        await(openEntered);

        runner.stop("task-stopping");
        allowOpen.countDown();

        StepVerifier.create(invocation.join())
                .expectErrorMatches(failure -> failure.getMessage()
                        .contains("stopped before runtime start"))
                .verify();
        assertEquals(1, probe.interrupts.get());
        assertEquals(1, probe.closes.get());
    }

    @Test
    void stopBeforeSubscriptionPreventsStaleExecutionAndCannotRemoveAReusedTask() {
        AtomicInteger staleSubscriptions = new AtomicInteger();
        RuntimeProbe stale = new RuntimeProbe(Flux.defer(() -> {
            staleSubscriptions.incrementAndGet();
            return Flux.error(new AssertionError("stale runtime executed"));
        }), false);
        RuntimeProbe fresh = new RuntimeProbe(Flux.never(), false);
        ConcurrentLinkedQueue<RuntimeProbe> planned = new ConcurrentLinkedQueue<>();
        planned.add(stale);
        planned.add(fresh);
        LiteFlowA2aAgentRunner runner = runner(
                new ProbeFactory(ignored -> planned.remove()));
        AgentRequestOptions request = options("task-reused", "session-1");
        Flux<?> stalePublisher = runner.stream(List.of(), request);

        runner.stop("task-reused");
        Flux<?> freshPublisher = runner.stream(List.of(), request);
        StepVerifier.create(stalePublisher)
                .expectErrorMatches(failure -> failure instanceof IllegalStateException
                        && failure.getMessage().contains("stopped before subscription"))
                .verify();
        Disposable freshSubscription = freshPublisher.subscribe();
        runner.stop("task-reused");
        freshSubscription.dispose();

        assertEquals(0, staleSubscriptions.get());
        assertEquals(1, stale.interrupts.get());
        assertEquals(1, stale.closes.get());
        assertEquals(1, fresh.interrupts.get());
        assertEquals(1, fresh.closes.get());
    }

    @Test
    void stopCannotCloseRuntimeWhileUpstreamSubscriptionIsBeingEstablished() {
        CountDownLatch sourceSubscribeEntered = new CountDownLatch(1);
        AtomicBoolean closedWhileSubscribing = new AtomicBoolean();
        RuntimeProbe[] probeHolder = new RuntimeProbe[1];
        Thread[] stopperHolder = new Thread[1];
        RuntimeProbe probe = new RuntimeProbe(Flux.defer(() -> {
            sourceSubscribeEntered.countDown();
            awaitStopAttempt(stopperHolder[0]);
            closedWhileSubscribing.set(probeHolder[0].closes.get() > 0);
            return Flux.never();
        }), false);
        probeHolder[0] = probe;
        LiteFlowA2aAgentRunner runner = runner(
                new ProbeFactory(ignored -> probe));
        Flux<?> publisher = runner.stream(
                List.of(), options("task-linearized", "session-linearized"));
        Thread stopper = new Thread(
                () -> runner.stop("task-linearized"), "a2a-stop-race");
        stopperHolder[0] = stopper;
        CompletableFuture<Disposable> subscription = CompletableFuture.supplyAsync(
                publisher::subscribe);
        await(sourceSubscribeEntered);
        stopper.start();

        Disposable disposable = subscription.join();
        join(stopper);
        disposable.dispose();

        assertFalse(closedWhileSubscribing.get(),
                "stop closed the runtime before source subscription was established");
        assertEquals(1, probe.interrupts.get());
        assertEquals(1, probe.closes.get());
    }

    private static LiteFlowA2aAgentRunner runner(A2aServerAgentFactory factory) {
        return new LiteFlowA2aAgentRunner(
                factory, "orders", "public-support", "guest");
    }

    private static AgentRequestOptions options(String taskId, String sessionId) {
        AgentRequestOptions options = new AgentRequestOptions();
        options.setTaskId(taskId);
        options.setSessionId(sessionId);
        options.setUserId("user-1");
        return options;
    }

    private static void await(CountDownLatch latch) {
        try {
            assertTrue(latch.await(5, TimeUnit.SECONDS), "timed out awaiting test barrier");
        }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError("test barrier interrupted", failure);
        }
    }

    private static void awaitStopAttempt(Thread stopper) {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
        while (stopper.getState() != Thread.State.BLOCKED
                && stopper.getState() != Thread.State.TERMINATED) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("stop did not reach its linearization point");
            }
            Thread.onSpinWait();
        }
    }

    private static void join(Thread thread) {
        try {
            thread.join(TimeUnit.SECONDS.toMillis(5));
            assertFalse(thread.isAlive(), "timed out joining stop thread");
        }
        catch (InterruptedException failure) {
            Thread.currentThread().interrupt();
            throw new AssertionError("stop thread join interrupted", failure);
        }
    }

    private static final class ProbeFactory implements A2aServerAgentFactory {
        private final Function<String, RuntimeProbe> runtimeProvider;
        private final AtomicInteger opens = new AtomicInteger();

        private ProbeFactory(Function<String, RuntimeProbe> runtimeProvider) {
            this.runtimeProvider = runtimeProvider;
        }

        @Override
        public String agentName() {
            return "support";
        }

        @Override
        public String agentDescription() {
            return "Support agent";
        }

        @Override
        public OwnedAgentRuntime open(AgentRequestOptions options) {
            opens.incrementAndGet();
            return runtimeProvider.apply(options.getTaskId());
        }
    }

    private static final class RuntimeProbe implements A2aServerAgentFactory.OwnedAgentRuntime {
        private final Flux<AgentEvent> events;
        private final boolean failClose;
        private final AtomicInteger interrupts = new AtomicInteger();
        private final AtomicInteger closes = new AtomicInteger();

        private RuntimeProbe(Flux<AgentEvent> events, boolean failClose) {
            this.events = events;
            this.failClose = failClose;
        }

        @Override
        public Flux<AgentEvent> stream(List<Msg> messages) {
            return events;
        }

        @Override
        public void interrupt() {
            interrupts.incrementAndGet();
        }

        @Override
        public void close() {
            closes.incrementAndGet();
            if (failClose) {
                throw new IllegalStateException("close failed");
            }
        }
    }
}
