package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshot;
import io.agentscope.harness.agent.sandbox.snapshot.SandboxSnapshotSpec;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class SessionSandboxRegistryTest {
    private final List<String> events = new CopyOnWriteArrayList<>();
    private final FakeSandboxClient client = new FakeSandboxClient(events);
    private final InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
    private final InMemoryAgentStateStore store = new InMemoryAgentStateStore();
    private final AtomicLong now = new AtomicLong();
    private final AgentConfig config = new AgentConfig();
    private final AgentInvocationGuard guard = new AgentInvocationGuardResolver().resolve(config);
    private final InvocationIdentityResolver identities = new InvocationIdentityResolver(UUID.randomUUID().toString());

    @Test
    void consecutiveTurnsReuseTheSameLiveContainerWithoutTakingSnapshots() {
        try (var registry = registry(snapshots)) {
            AgentInvocationIdentity id = id("a");
            Sandbox first = turn(registry, id, sandbox -> {
                sandbox.exec(context(id), "write answer.txt preserved", 1);
                return sandbox;
            });
            Sandbox second = turn(registry, id, sandbox -> sandbox);
            assertSame(first, second);
            assertEquals("preserved", ((FakeSandbox) second).file("answer.txt"));
            assertEquals(1, client.createAttempts());
            assertEquals(0, snapshots.snapshotCount());
            assertFalse(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
        }
        assertEquals(1, snapshots.snapshotCount());
        assertEquals(1, events.stream().filter(event -> event.startsWith("shutdown:")).count());
    }

    @Test
    void idleTimerStartsAfterCompletionAndEvictionRestoresFilesOnTheNextTurn() {
        try (var registry = registry(snapshots)) {
            AgentInvocationIdentity id = id("a");
            Sandbox first = turn(registry, id, sandbox -> {
                sandbox.exec(context(id), "write answer.txt retained", 1);
                now.set(Duration.ofHours(1).toNanos());
                return sandbox;
            });
            registry.evictIdle();
            assertFalse(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
            now.addAndGet(Duration.ofMinutes(10).toNanos());
            registry.evictIdle();
            assertEquals(1, snapshots.snapshotCount());
            Sandbox restored = turn(registry, id, sandbox -> sandbox);
            assertNotSame(first, restored);
            assertEquals("retained", ((FakeSandbox) restored).file("answer.txt"));
            assertEquals(1, client.resumedStates().size());
        }
    }

    @Test
    void activeAndCancelledTurnsKeepTheirLeaseUntilTermination() {
        try (var registry = registry(snapshots)) {
            AgentInvocationIdentity id = id("a");
            RuntimeContext ctx = context(id);
            try (var lease = guard.acquire(AgentInvocationKey.workspace(id), Duration.ZERO)) {
                var subscription = registry.execute(id, ctx, () -> {
                    try { ctx.get(SandboxContext.class).getExternalSandbox().start(); }
                    catch (Exception failure) { return Mono.error(failure); }
                    return Mono.never();
                }).subscribe();
                now.set(Duration.ofHours(1).toNanos());
                java.util.concurrent.CompletableFuture.runAsync(registry::evictIdle).join();
                registry.evictIdle();
                assertFalse(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
                subscription.dispose();
                assertNull(ctx.get(SandboxContext.class));
            }
            registry.evictIdle();
            assertFalse(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
            now.addAndGet(Duration.ofMinutes(10).toNanos());
            registry.evictIdle();
            assertTrue(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
        }
    }

    @Test
    void capacityEvictsAnIdleConversationAndNeverMixesItsFilesWithAnother() {
        config.getHarness().getDocker().setMaxCachedSandboxes(1);
        try (var registry = registry(snapshots)) {
            turn(registry, id("a"), sandbox -> sandbox.exec(context(id("a")), "write private.txt alice", 1));
            turn(registry, id("b"), sandbox -> {
                assertNull(((FakeSandbox) sandbox).file("private.txt"));
                return null;
            });
            assertEquals(2, client.createAttempts());
            assertEquals(1, snapshots.snapshotCount());
            turn(registry, id("a"), sandbox -> {
                assertEquals("alice", ((FakeSandbox) sandbox).file("private.txt"));
                return null;
            });
        }
    }

    @Test
    void snapshotFailureDoesNotDestroyTheOnlyCopyAndCanBeRetried() {
        AtomicBoolean fail = new AtomicBoolean(true);
        SandboxSnapshotSpec flaky = key -> {
            SandboxSnapshot delegate = snapshots.build(key);
            return new SandboxSnapshot() {
                public void persist(InputStream archive) throws Exception {
                    if (fail.get()) throw new IllegalStateException("storage unavailable");
                    delegate.persist(archive);
                }
                public InputStream restore() throws Exception { return delegate.restore(); }
                public boolean isRestorable() throws Exception { return delegate.isRestorable(); }
                public String getId() { return delegate.getId(); }
                public String getType() { return delegate.getType(); }
            };
        };
        try (var registry = registry(flaky)) {
            turn(registry, id("a"), sandbox -> sandbox.exec(context(id("a")), "write valuable.txt saved", 1));
            now.set(Duration.ofHours(1).toNanos());
            registry.evictIdle();
            assertEquals("saved", client.latestSandbox().file("valuable.txt"));
            assertFalse(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
            fail.set(false);
            registry.evictIdle();
            assertEquals(1, snapshots.snapshotCount());
            assertTrue(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
        }
    }

    @Test
    void deletingAConversationReleasesItsLiveSandbox() {
        AgentInvocationIdentity id = id("delete");
        config.getRuntime().setNamespace(id.namespace());
        try (var registry = registry(snapshots); var conversations = new AgentConversationService(config, store)) {
            conversations.create(id.userId(), id.conversationId(), "test", false);
            turn(registry, id, sandbox -> sandbox);
            conversations.delete(id.userId(), id.conversationId());
            assertTrue(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
            assertTrue(conversations.get(id.userId(), id.conversationId()).isEmpty());
        }
    }

    @Test
    void handoverBetweenAgentRuntimesCheckpointsThePreviousOwner() {
        try (var first = registry(snapshots); var second = registry(snapshots)) {
            AgentInvocationIdentity id = id("a");
            Sandbox old = turn(first, id, sandbox -> {
                sandbox.exec(context(id), "write shared.txt handover", 1);
                return sandbox;
            });
            Sandbox next = turn(second, identities.resolve("user", "a", "other-agent"), sandbox -> sandbox);
            assertNotSame(old, next);
            assertEquals("handover", ((FakeSandbox) next).file("shared.txt"));
            assertEquals(1, snapshots.snapshotCount());
        }
    }

    @Test
    void modelFailureReleasesTheLeaseAndKeepsTheSandboxReusable() {
        try (var registry = registry(snapshots)) {
            var id = id("error");
            assertThrows(RuntimeException.class, () -> turn(registry, id, sandbox -> { throw new IllegalStateException("model failed"); }));
            Sandbox first = client.latestSandbox();
            assertSame(first, turn(registry, id, sandbox -> sandbox));
        }
    }

    @Test
    void cancellationDuringStateLoadDoesNotLeaveAnUnreleasableCacheEntry() throws Exception {
        var entered = new java.util.concurrent.CountDownLatch(1);
        var proceed = new java.util.concurrent.CountDownLatch(1);
        var blockOnce = new AtomicBoolean(true);
        var blockingStore = new InMemoryAgentStateStore() {
            @Override public <T extends io.agentscope.core.state.State> java.util.Optional<T> get(
                    String user, String session, String key, Class<T> type) {
                if (blockOnce.compareAndSet(true, false)) {
                    entered.countDown();
                    boolean finished = false;
                    while (!finished) {
                        try { proceed.await(); finished = true; }
                        catch (InterruptedException ignored) { /* Simulate a non-cancellable store call. */ }
                    }
                }
                return super.get(user, session, key, type);
            }
        };
        var worker = java.util.concurrent.Executors.newSingleThreadExecutor();
        try (var registry = registry(snapshots, blockingStore)) {
            var id = id("cancel-load");
            var ctx = context(id);
            try (var lease = guard.acquire(AgentInvocationKey.workspace(id), Duration.ZERO)) {
                var subscription = registry.execute(id, ctx, () -> Mono.never())
                        .subscribeOn(reactor.core.scheduler.Schedulers.fromExecutor(worker)).subscribe();
                assertTrue(entered.await(2, java.util.concurrent.TimeUnit.SECONDS));
                subscription.dispose();
                proceed.countDown();
                worker.submit(() -> {}).get(2, java.util.concurrent.TimeUnit.SECONDS);
            }
            assertTrue(events.stream().anyMatch(event -> event.startsWith("shutdown:")));
            assertNotNull(turn(registry, id, sandbox -> sandbox));
        } finally {
            proceed.countDown();
            worker.shutdownNow();
        }
    }

    private SessionSandboxRegistry registry(SandboxSnapshotSpec snapshot) {
        return registry(snapshot, store);
    }

    private SessionSandboxRegistry registry(SandboxSnapshotSpec snapshot, InMemoryAgentStateStore stateStore) {
        WorkspaceSpec workspace = new WorkspaceSpec();
        workspace.setRoot("/workspace");
        SandboxContext template = SandboxContext.builder().client(client).workspaceSpec(workspace)
                .clientOptions(new DockerSandboxClientOptions()).snapshotSpec(snapshot)
                .isolationScope(IsolationScope.SESSION).build();
        return new SessionSandboxRegistry(template, stateStore, "agent", guard, config.getHarness().getDocker(), now::get, false);
    }

    private AgentInvocationIdentity id(String session) { return identities.resolve("user", session, "agent"); }
    private RuntimeContext context(AgentInvocationIdentity id) {
        return RuntimeContext.builder().userId(id.userId()).sessionId(id.runtimeSessionId()).build();
    }
    private <T> T turn(SessionSandboxRegistry registry, AgentInvocationIdentity id, SandboxAction<T> action) {
        RuntimeContext ctx = context(id);
        try (var lease = guard.acquire(AgentInvocationKey.workspace(id), Duration.ZERO)) {
            return registry.execute(id, ctx, () -> Mono.fromCallable(() -> {
                Sandbox sandbox = ctx.get(SandboxContext.class).getExternalSandbox();
                sandbox.start();
                return action.run(sandbox);
            })).block(Duration.ofSeconds(2));
        }
    }
    private interface SandboxAction<T> { T run(Sandbox sandbox) throws Exception; }
}
