package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/** Fair, process-local invocation guard shared by application components. */
public final class LocalAgentInvocationGuard implements AgentInvocationGuard {

    private final ConcurrentHashMap<AgentInvocationKey, Entry> entries = new ConcurrentHashMap<>();

    @Override
    public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(timeout, "timeout");
        if (timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must not be negative");
        }

        Entry entry = entries.compute(key, (ignored, existing) -> {
            Entry value = existing == null ? new Entry() : existing;
            value.registrations.incrementAndGet();
            value.waiters.incrementAndGet();
            return value;
        });
        try {
            if (!entry.lock.tryLock(timeout.toNanos(), TimeUnit.NANOSECONDS)) {
                releaseRegistration(key, entry);
                throw new AgentInvocationException(AgentInvocationErrorType.TIMEOUT,
                        "Timed out acquiring invocation lease for " + key);
            }
            entry.waiters.decrementAndGet();
            return new LocalLease(key, entry);
        } catch (InterruptedException exception) {
            releaseRegistration(key, entry);
            Thread.currentThread().interrupt();
            throw new AgentInvocationException(AgentInvocationErrorType.INTERRUPTED,
                    "Interrupted while acquiring invocation lease for " + key, exception);
        } catch (RuntimeException exception) {
            if (!(exception instanceof AgentInvocationException)) {
                releaseRegistration(key, entry);
            }
            throw exception;
        }
    }

    boolean isTracked(AgentInvocationKey key) {
        return entries.containsKey(key);
    }

    int waiterCount(AgentInvocationKey key) {
        Entry entry = entries.get(key);
        return entry == null ? 0 : entry.waiters.get();
    }

    private void releaseRegistration(AgentInvocationKey key, Entry entry) {
        entry.waiters.updateAndGet(value -> value > 0 ? value - 1 : 0);
        if (entry.registrations.decrementAndGet() == 0) {
            entries.remove(key, entry);
        }
    }

    private final class LocalLease implements AgentInvocationLease {
        private final AgentInvocationKey key;
        private final Entry entry;
        private final AtomicBoolean closed = new AtomicBoolean();

        private LocalLease(AgentInvocationKey key, Entry entry) {
            this.key = key;
            this.entry = entry;
        }

        @Override
        public AgentInvocationKey key() {
            return key;
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                entry.lock.unlock();
                releaseRegistration(key, entry);
            }
        }
    }

    private static final class Entry {
        private final ReentrantLock lock = new ReentrantLock(true);
        private final AtomicInteger registrations = new AtomicInteger();
        private final AtomicInteger waiters = new AtomicInteger();
    }
}
