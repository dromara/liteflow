package com.yomahub.liteflow.agent.redis.storage;

import com.yomahub.liteflow.agent.guard.*;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Token-checked leases with renewal; closing a former owner never releases a successor's lease. */
public final class RedisInvocationGuardProvider implements AgentInvocationGuardProvider {
    private static final String ACQUIRE = "if redis.call('set',KEYS[1],ARGV[1],'NX','PX',ARGV[2]) then return 1 else return 0 end";
    private static final String RENEW = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('pexpire',KEYS[1],ARGV[2]) else return 0 end";
    private static final String RELEASE = "if redis.call('get',KEYS[1])==ARGV[1] then return redis.call('del',KEYS[1]) else return 0 end";

    @Override public AgentStateStoreType type() { return AgentStateStoreType.REDIS; }
    @Override public AgentInvocationGuard resolve(AgentConfig config) {
        Duration duration = config.getInvocationGuard().getLeaseDuration();
        if (duration == null || duration.toMillis() < 300) throw new AgentConfigException("Redis invocation lease-duration must be at least 300ms");
        long ttl = duration.toMillis();
        var connection = RedisStorageConnection.open(config.getStateStore().getRedis());
        String prefix = RedisStorageConnection.prefix(config.getStateStore().getRedis()) + "lock:";
        ScheduledExecutorService renewals = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "liteflow-redis-lease-renewal"); thread.setDaemon(true); return thread;
        });
        return new AgentInvocationGuard() {
            private final AtomicBoolean closed = new AtomicBoolean();
            public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
                if (closed.get()) throw new IllegalStateException("Invocation guard is closed");
                if (timeout == null || timeout.isNegative()) throw new IllegalArgumentException("timeout must be nonnegative");
                try {
                    String address = prefix + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                            .digest(key.toString().getBytes(StandardCharsets.UTF_8)));
                    String owner = UUID.randomUUID().toString();
                    long deadline = System.nanoTime() + timeout.toNanos();
                    do {
                        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
                        long acquired = connection.eval.run(ACQUIRE, List.of(address), List.of(owner, Long.toString(ttl)));
                        if (acquired == 1) return lease(key, address, owner);
                        if (System.nanoTime() >= deadline) break;
                        Thread.sleep(Math.min(25, Math.max(1, TimeUnit.NANOSECONDS.toMillis(deadline - System.nanoTime()))));
                    } while (true);
                    throw new AgentInvocationException(AgentInvocationErrorType.TIMEOUT, "Timed out acquiring Redis invocation lease");
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw new AgentInvocationException(AgentInvocationErrorType.INTERRUPTED, "Redis lease acquisition interrupted", interrupted);
                } catch (AgentInvocationException failure) { throw failure; }
                catch (Exception failure) { throw new AgentInvocationException("Cannot acquire Redis invocation lease", failure); }
            }
            private AgentInvocationLease lease(AgentInvocationKey key, String address, String owner) {
                Thread caller = Thread.currentThread();
                AtomicBoolean released = new AtomicBoolean();
                AtomicBoolean lost = new AtomicBoolean();
                ScheduledFuture<?> renewal = renewals.scheduleWithFixedDelay(() -> {
                    synchronized (released) {
                        if (released.get() || lost.get()) return;
                        try {
                            if (connection.eval.run(RENEW, List.of(address), List.of(owner, Long.toString(ttl))) == 1) return;
                        } catch (RuntimeException failure) { /* Fail the active invocation if renewal cannot be confirmed. */ }
                        lost.set(true); caller.interrupt();
                    }
                }, ttl / 3, ttl / 3, TimeUnit.MILLISECONDS);
                return new AgentInvocationLease() {
                    public AgentInvocationKey key() { return key; }
                    public void close() {
                        synchronized (released) {
                            if (!released.compareAndSet(false, true)) return;
                            renewal.cancel(false);
                            connection.eval.run(RELEASE, List.of(address), List.of(owner));
                            if (lost.get()) throw new AgentInvocationException("Redis invocation lease was lost");
                        }
                    }
                };
            }
            public void close() {
                if (!closed.compareAndSet(false, true)) return;
                renewals.shutdownNow(); connection.close();
            }
        };
    }
}
