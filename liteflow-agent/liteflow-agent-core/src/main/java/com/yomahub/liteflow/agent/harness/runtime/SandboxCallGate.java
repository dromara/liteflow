package com.yomahub.liteflow.agent.harness.runtime;

import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Fair reactive capacity-one gate for an AgentScope sandbox lifecycle middleware instance.
 *
 * <p>LiteFlow's session sandbox registry and managed filesystem still share a fallback sandbox
 * reference. Keep their full public calls serialized even though AgentScope 2.0.3 also supports
 * per-call bindings. Waiting is reactive and cancellation-aware; no calling thread is blocked.
 */
public final class SandboxCallGate implements AutoCloseable {

    private final Deque<Waiter> waiters = new ArrayDeque<>();
    private boolean held;
    private boolean closed;

    /** Runs one publisher while holding the gate until complete, error, or cancellation. */
    public <T> Mono<T> execute(Supplier<Mono<T>> invocation) {
        Objects.requireNonNull(invocation, "invocation");
        return Mono.usingWhen(
                acquire(),
                ignored -> Mono.defer(() -> Objects.requireNonNull(
                        invocation.get(), "sandbox invocation must not return null")),
                Lease::release,
                (lease, failure) -> lease.release(),
                Lease::release);
    }

    private Mono<Lease> acquire() {
        return Mono.defer(() -> {
            Waiter waiter = new Waiter();
            boolean grant = false;
            boolean reject = false;
            synchronized (this) {
                if (closed) {
                    waiter.terminal = true;
                    reject = true;
                }
                else if (!held && waiters.isEmpty()) {
                    held = true;
                    waiter.granted = true;
                    grant = true;
                }
                else {
                    waiter.queued = true;
                    waiters.addLast(waiter);
                }
            }
            if (reject) {
                waiter.sink.tryEmitError(closedFailure());
            }
            else if (grant) {
                emit(waiter);
            }
            return waiter.sink.asMono().doOnCancel(() -> cancel(waiter));
        });
    }

    private void cancel(Waiter waiter) {
        boolean release = false;
        synchronized (this) {
            if (waiter.terminal || waiter.cancelled) {
                return;
            }
            waiter.cancelled = true;
            if (waiter.queued) {
                waiters.remove(waiter);
                waiter.queued = false;
            }
            else if (waiter.granted) {
                release = true;
            }
        }
        if (release) {
            release(waiter);
        }
    }

    private void emit(Waiter waiter) {
        Sinks.EmitResult result = waiter.sink.tryEmitValue(new Lease(this, waiter));
        if (result == Sinks.EmitResult.FAIL_CANCELLED) {
            cancel(waiter);
        }
    }

    private Mono<Void> release(Waiter owner) {
        Waiter next = null;
        synchronized (this) {
            if (owner.terminal || !owner.granted) {
                return Mono.empty();
            }
            owner.terminal = true;
            owner.granted = false;
            held = false;
            if (!closed) {
                while (!waiters.isEmpty()) {
                    Waiter candidate = waiters.removeFirst();
                    candidate.queued = false;
                    if (!candidate.cancelled && !candidate.terminal) {
                        held = true;
                        candidate.granted = true;
                        next = candidate;
                        break;
                    }
                }
            }
        }
        if (next != null) {
            emit(next);
        }
        return Mono.empty();
    }

    @Override
    public void close() {
        List<Waiter> rejected = new ArrayList<>();
        synchronized (this) {
            if (closed) {
                return;
            }
            closed = true;
            while (!waiters.isEmpty()) {
                Waiter waiter = waiters.removeFirst();
                waiter.queued = false;
                if (!waiter.cancelled && !waiter.terminal) {
                    waiter.terminal = true;
                    rejected.add(waiter);
                }
            }
        }
        IllegalStateException failure = closedFailure();
        rejected.forEach(waiter -> waiter.sink.tryEmitError(failure));
    }

    private static IllegalStateException closedFailure() {
        return new IllegalStateException("Sandbox call gate is closed");
    }

    private static final class Waiter {
        private final Sinks.One<Lease> sink = Sinks.one();
        private boolean queued;
        private boolean granted;
        private boolean cancelled;
        private boolean terminal;
    }

    private static final class Lease {
        private final SandboxCallGate gate;
        private final Waiter owner;
        private final AtomicBoolean released = new AtomicBoolean();

        private Lease(SandboxCallGate gate, Waiter owner) {
            this.gate = gate;
            this.owner = owner;
        }

        private Mono<Void> release() {
            return released.compareAndSet(false, true)
                    ? gate.release(owner)
                    : Mono.empty();
        }
    }
}
