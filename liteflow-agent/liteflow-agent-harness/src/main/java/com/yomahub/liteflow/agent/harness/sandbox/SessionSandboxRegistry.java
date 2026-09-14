package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.conversation.AgentConversationResourceRegistry;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.property.agent.DockerSandboxConfig;
import com.yomahub.liteflow.property.agent.DockerSandboxLifecycle;
import com.yomahub.liteflow.agent.harness.storage.ManagedSandboxFilesystem;
import io.agentscope.harness.agent.sandbox.snapshot.RemoteSandboxSnapshot;
import java.util.function.Consumer;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.SandboxIsolationKey;
import io.agentscope.harness.agent.sandbox.SandboxState;
import io.agentscope.harness.agent.sandbox.SessionSandboxStateStore;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Component-owned cache of leased, conversation-scoped Docker sandboxes. The LiteFlow workspace
 * invocation guard protects all calls and background eviction, including handover between Agents.
 * Shared-storage mode checkpoints each call; stale process-local handles cannot overwrite a newer owner.
 */
public final class SessionSandboxRegistry implements AutoCloseable {
    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(SessionSandboxRegistry.class);
    private final Map<AgentInvocationKey, Entry> entries = new LinkedHashMap<>();
    private final SandboxContext template;
    private final SandboxClient<DockerSandboxClientOptions> client;
    private final SessionSandboxStateStore stateStore;
    private final AgentInvocationGuard guard;
    private final long idleNanos;
    private final int capacity;
    private final LongSupplier clock;
    private final ScheduledExecutorService evictor;
    private boolean closed;
    private final ManagedSandboxFilesystem managedFilesystem;
    private final Consumer<RuntimeContext> beforeStart;
    private final boolean perCall;

    public SessionSandboxRegistry(SandboxContext template, AgentStateStore store, String agentId,
                                  AgentInvocationGuard guard, DockerSandboxConfig config) {
        this(template, store, agentId, guard, config, System::nanoTime, true, null, context -> { });
    }

    @SuppressWarnings("unchecked")
    SessionSandboxRegistry(SandboxContext template, AgentStateStore store, String agentId,
                           AgentInvocationGuard guard, DockerSandboxConfig config,
                           LongSupplier clock, boolean schedule) {
        this(template, store, agentId, guard, config, clock, schedule, null, context -> { });
    }

    public SessionSandboxRegistry(SandboxContext template, AgentStateStore store, String agentId,
            AgentInvocationGuard guard, DockerSandboxConfig config, ManagedSandboxFilesystem managedFilesystem,
            Consumer<RuntimeContext> beforeStart) {
        this(template, store, agentId, guard, config, System::nanoTime, true, managedFilesystem, beforeStart);
    }

    @SuppressWarnings("unchecked")
    SessionSandboxRegistry(SandboxContext template, AgentStateStore store, String agentId,
            AgentInvocationGuard guard, DockerSandboxConfig config, LongSupplier clock, boolean schedule,
            ManagedSandboxFilesystem managedFilesystem, Consumer<RuntimeContext> beforeStart) {
        config.validate();
        this.managedFilesystem = managedFilesystem;
        this.beforeStart = beforeStart;
        this.perCall = managedFilesystem != null && config.getLifecycle() == DockerSandboxLifecycle.PER_CALL;
        if (template.getSnapshotSpec() == null
                || template.getSnapshotSpec() instanceof io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec) {
            throw new AgentConfigException(
                    "SESSION_IDLE requires docker.snapshot-root or a persistent SandboxSnapshotProvider");
        }
        this.template = template;
        this.client = (SandboxClient<DockerSandboxClientOptions>) template.getClient();
        this.stateStore = new SessionSandboxStateStore(store, agentId);
        this.guard = guard;
        this.idleNanos = config.getIdleTimeout().toNanos();
        this.capacity = config.getMaxCachedSandboxes();
        this.clock = clock;
        this.evictor = schedule ? Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "liteflow-sandbox-evictor");
            thread.setDaemon(true);
            return thread;
        }) : null;
        if (evictor != null) {
            long interval = config.getEvictionInterval().toMillis();
            evictor.scheduleWithFixedDelay(this::evictIdle, interval, interval, TimeUnit.MILLISECONDS);
        }
    }

    /** Runs inside the component's invocation leases and its SandboxCallGate. */
    public <T> Mono<T> execute(AgentInvocationIdentity identity, RuntimeContext context, Supplier<Mono<T>> call) {
        return Mono.usingWhen(
                Mono.fromCallable(() -> borrow(identity, context)),
                entry -> Mono.defer(() -> {
                    context.put(SandboxContext.class, SandboxContext.builder()
                            .client(client).clientOptions(template.getClientOptions())
                            .workspaceSpec(template.getWorkspaceSpec()).snapshotSpec(template.getSnapshotSpec())
                            .isolationScope(IsolationScope.SESSION).externalSandbox(entry.sandbox).build());
                    if (managedFilesystem != null) {
                        beforeStart.accept(context);
                        try { entry.sandbox.start(); }
                        catch (Exception failure) { return Mono.error(failure); }
                        entry.checkpointed = false;
                        managedFilesystem.setSandbox(entry.sandbox);
                    }
                    return call.get();
                }),
                entry -> release(entry, context),
                (entry, failure) -> release(entry, context),
                entry -> release(entry, context))
                .doOnDiscard(Entry.class, this::discard);
    }

    private Entry borrow(AgentInvocationIdentity identity, RuntimeContext context) throws Exception {
        if (!Objects.equals(identity.userId(), context.getUserId())
                || !Objects.equals(identity.runtimeSessionId(), context.getSessionId())) {
            throw new AgentConfigException("Sandbox runtime identity must match the LiteFlow invocation");
        }
        AgentInvocationKey key = AgentInvocationKey.workspace(identity);
        Entry existing;
        synchronized (this) { requireOpen(); existing = entries.get(key); }
        if (existing != null && managedFilesystem != null && stale(existing)) {
            discardStale(existing);
            existing = null;
        }
        if (existing != null) {
            synchronized (this) {
                if (existing.busy) throw new IllegalStateException("Sandbox is already in use");
                existing.busy = true;
                return existing;
            }
        }
        // A different Agent component may own this conversation's workspace. Save it before
        // restoring through this component's current Docker/skill configuration.
        AgentConversationResourceRegistry.release(key);
        makeRoom();
        SandboxIsolationKey stateKey = SandboxIsolationKey.resolve(IsolationScope.SESSION, context, identity.agentNamespace())
                .orElseThrow(() -> new IllegalArgumentException("Sandbox session identity is missing"));
        Sandbox sandbox;
        var saved = stateStore.load(stateKey);
        if (saved.isPresent()) {
            SandboxState state = client.deserializeState(saved.get(), template.getSnapshotSpec());
            if (managedFilesystem != null && !(state.getSnapshot() instanceof RemoteSandboxSnapshot)) {
                if (!(state.getSnapshot() instanceof io.agentscope.harness.agent.sandbox.snapshot.LocalSandboxSnapshot)) {
                    throw new AgentConfigException("Unsupported legacy snapshot type; migrate to shared storage before resuming");
                }
                var remote = template.getSnapshotSpec().build(state.getSessionId());
                com.yomahub.liteflow.agent.harness.storage.LegacySnapshotMigration.migrate(
                        state.getSnapshot(), remote, managedFilesystem, context, managedFilesystem.maxFileBytes());
                // Remove the old container only after both its records and business archive are durable.
                client.resume(state).shutdown();
                state.setSnapshot(remote);
                state.setWorkspaceRootReady(false);
            }
            if (managedFilesystem != null && !state.getSnapshot().isRestorable()) {
                throw new AgentConfigException("Shared sandbox snapshot is missing; refusing to replace the existing workspace with an empty one");
            }
            state.setWorkspaceSpec(template.getWorkspaceSpec().copy());
            if (state.getSnapshot() == null || !state.getSnapshot().isPersistenceEnabled()) {
                state.setSnapshot(template.getSnapshotSpec().build(state.getSessionId()));
            }
            sandbox = client.resume(state);
        } else {
            sandbox = client.create(template.getWorkspaceSpec().copy(), template.getSnapshotSpec(),
                    (DockerSandboxClientOptions) template.getClientOptions());
        }
        Entry entry = new Entry(key, stateKey, sandbox, saved.isEmpty());
        entry.lastPersisted = saved.orElse(null);
        try {
            synchronized (this) {
                requireOpen();
                if (entries.size() >= capacity) throw new IllegalStateException("Sandbox cache capacity reached");
                AgentConversationResourceRegistry.register(key, entry);
                entries.put(key, entry);
            }
            return entry;
        } catch (RuntimeException failure) {
            if (entry.fresh) {
                try { sandbox.shutdown(); } catch (Exception cleanup) { failure.addSuppressed(cleanup); }
            }
            throw failure;
        }
    }

    private void discard(Entry entry) {
        // Cancellation may outlive a non-cancellable state load. The agent never started this
        // handle; do not persist stale metadata or stop a resumed container another turn owns.
        synchronized (this) {
            entries.remove(entry.key, entry);
            AgentConversationResourceRegistry.unregister(entry.key, entry);
        }
        if (entry.fresh) {
            try { entry.sandbox.shutdown(); }
            catch (Exception failure) { LOG.warn("Discarded sandbox cleanup failed", failure); }
        }
    }

    private Mono<Void> release(Entry entry, RuntimeContext context) {
        return Mono.fromRunnable(() -> releaseNow(entry, context));
    }

    private void releaseNow(Entry entry, RuntimeContext context) {
        context.put(SandboxContext.class, null);
        try {
            if (managedFilesystem != null) {
                managedFilesystem.setSandbox(null);
                if (entry.sandbox.isRunning()) {
                    entry.sandbox.stop();
                    entry.checkpointed = true;
                    persist(entry);
                }
            } else if (entry.sandbox.isRunning()) persist(entry);
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot checkpoint sandbox to shared storage", failure);
        } finally {
            synchronized (this) {
                entry.busy = false;
                entry.lastUsed = clock.getAsLong();
            }
        }
        // Failed/pre-cancelled starts have no reusable workspace.
        if (perCall || (!entry.sandbox.isRunning() && !entry.checkpointed)) {
            try { evict(entry); } catch (Exception failure) { throw new IllegalStateException("Sandbox cleanup failed", failure); }
        }
    }

    private void persist(Entry entry) {
        try {
            String serialized = client.serializeState(entry.sandbox.getState());
            stateStore.save(entry.stateKey, serialized);
            entry.lastPersisted = serialized;
        } catch (Exception failure) {
            throw new IllegalStateException("Cannot persist sandbox resume state", failure);
        }
    }

    private void makeRoom() throws Exception {
        for (Entry entry : snapshot()) {
            synchronized (this) {
                if (entries.size() < capacity) return;
            }
            try (var lease = guard.acquire(entry.key, Duration.ZERO)) {
                evict(entry);
            } catch (AgentInvocationException busy) {
                if (busy.getErrorType() != AgentInvocationErrorType.TIMEOUT) throw busy;
                // A queued or active invocation wins over capacity eviction.
            }
        }
        synchronized (this) {
            if (entries.size() >= capacity) {
                throw new IllegalStateException("Sandbox cache is full; all cached sandboxes are in use");
            }
        }
    }

    /** A failed checkpoint keeps the container for a later retry; it is never destroyed first. */
    public void evictIdle() {
        for (Entry entry : snapshot()) {
            try (var lease = guard.acquire(entry.key, Duration.ZERO)) {
                synchronized (this) {
                    if (entry.busy || clock.getAsLong() - entry.lastUsed < idleNanos) continue;
                }
                evict(entry);
            } catch (AgentInvocationException busy) {
                if (busy.getErrorType() != AgentInvocationErrorType.TIMEOUT) {
                    LOG.warn("Sandbox eviction could not acquire its workspace lease", busy);
                }
                // Skip active work, including a request that has not reached borrow() yet.
            } catch (Exception failure) {
                LOG.warn("Sandbox idle eviction failed; keeping the container for retry", failure);
            }
        }
    }

    private synchronized ArrayList<Entry> snapshot() {
        ArrayList<Entry> result = new ArrayList<>(entries.values());
        result.sort(Comparator.comparingLong(entry -> entry.lastUsed));
        return result;
    }

    /** Caller owns the workspace guard. No global cache monitor is held during Docker/storage IO. */
    private void evict(Entry entry) throws Exception {
        synchronized (this) {
            if (entries.get(entry.key) != entry) return;
            if (entry.busy) throw new IllegalStateException("Cannot evict an active sandbox");
        }
        if (managedFilesystem != null && stale(entry)) {
            discardStale(entry);
            return;
        }
        if (entry.sandbox.isRunning()) {
            entry.sandbox.stop();
            entry.checkpointed = true;
        }
        // A failed fresh start has no durable workspace to advertise to another node.
        if (entry.checkpointed || entry.lastPersisted != null || entry.sandbox.isRunning()) persist(entry);
        entry.sandbox.shutdown();
        synchronized (this) {
            entries.remove(entry.key, entry);
            AgentConversationResourceRegistry.unregister(entry.key, entry);
        }
    }

    private boolean stale(Entry entry) throws java.io.IOException {
        return !Objects.equals(entry.lastPersisted, stateStore.load(entry.stateKey).orElse(null));
    }

    private void discardStale(Entry entry) throws Exception {
        // The workspace lease excludes active calls. Never checkpoint this old workspace.
        entry.sandbox.shutdown();
        synchronized (this) {
            entries.remove(entry.key, entry);
            AgentConversationResourceRegistry.unregister(entry.key, entry);
        }
    }

    @Override
    public void close() {
        synchronized (this) { closed = true; }
        if (evictor != null) evictor.shutdownNow();
        RuntimeException failure = null;
        for (Entry entry : snapshot()) {
            try (var lease = guard.acquire(entry.key, Duration.ofSeconds(30))) {
                evict(entry);
            } catch (Exception cleanup) {
                // Leave the physical container available for recovery if its checkpoint failed.
                AgentConversationResourceRegistry.unregister(entry.key, entry);
                if (failure == null) failure = new IllegalStateException("Cannot close cached sandboxes", cleanup);
                else failure.addSuppressed(cleanup);
            }
        }
        if (failure != null) throw failure;
    }

    private void requireOpen() {
        if (closed) throw new IllegalStateException("Sandbox registry is closed");
    }

    private final class Entry implements AutoCloseable {
        private final AgentInvocationKey key;
        private final SandboxIsolationKey stateKey;
        private final Sandbox sandbox;
        private final boolean fresh;
        private boolean busy = true;
        private boolean checkpointed;
        private String lastPersisted;
        private long lastUsed;

        private Entry(AgentInvocationKey key, SandboxIsolationKey stateKey, Sandbox sandbox, boolean fresh) {
            this.key = key;
            this.stateKey = stateKey;
            this.sandbox = sandbox;
            this.fresh = fresh;
        }

        @Override public void close() throws Exception { evict(this); }
    }
}
