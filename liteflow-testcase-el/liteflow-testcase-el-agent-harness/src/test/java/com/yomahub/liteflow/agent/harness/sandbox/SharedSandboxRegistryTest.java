package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.guard.*;
import com.yomahub.liteflow.agent.harness.storage.*;
import com.yomahub.liteflow.property.agent.*;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.filesystem.remote.store.InMemoryStore;
import io.agentscope.harness.agent.sandbox.*;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.snapshot.RemoteSnapshotSpec;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Mono;
import java.nio.file.Path;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import static org.junit.jupiter.api.Assertions.*;

class SharedSandboxRegistryTest {
    @TempDir Path temp;
    @Test void eachTurnCheckpointsAndStaleEvictionCannotOverwriteTheNewOwner() throws Exception {
        var events = new ArrayList<String>(); var client = new FakeSandboxClient(events);
        var records = new InMemoryStore(); var states = new InMemoryAgentStateStore();
        var config = new AgentConfig(); config.getHarness().getDocker().setLifecycle(DockerSandboxLifecycle.SESSION_IDLE);
        var guard = new AgentInvocationGuardResolver().resolve(config);
        var id = new InvocationIdentityResolver(UUID.randomUUID().toString()).resolve("user", "session", "agent");
        var rc = RuntimeContext.builder().userId(id.userId()).sessionId(id.runtimeSessionId()).build();
        var fs = new ManagedSandboxFilesystem(records, id.namespace(), id.agentNamespace(), temp, "/workspace");
        var spec = new RemoteSnapshotSpec(new StoreSnapshotClient(records, id.namespace()));
        var template = SandboxContext.builder().client(client).clientOptions(new DockerSandboxClientOptions())
                .workspaceSpec(new WorkspaceSpec()).snapshotSpec(spec).isolationScope(IsolationScope.SESSION).build();
        AtomicLong clock = new AtomicLong();
        try (var registry = new SessionSandboxRegistry(template, states, id.agentNamespace(), guard,
                config.getHarness().getDocker(), clock::get, false, fs, ignored -> {})) {
            try (var held = guard.acquire(AgentInvocationKey.workspace(id), Duration.ZERO)) {
                registry.execute(id, rc, () -> Mono.fromCallable(() -> {
                    assertNotNull(fs.getSandbox());
                    fs.getSandbox().exec(rc, "write result.txt first", 1);
                    fs.write(rc, "MEMORY.md", "remote memory");
                    return "answer";
                })).block();
                assertNull(fs.getSandbox());
                assertFalse(events.stream().anyMatch(e -> e.startsWith("shutdown:")));
                Sandbox first = client.latestSandbox();
                assertTrue(first.getState().getSnapshot().isRestorable());
                registry.execute(id, rc, () -> Mono.fromCallable(() -> {
                    assertSame(first, fs.getSandbox()); return "second";
                })).block();
                var key = SandboxIsolationKey.resolve(IsolationScope.SESSION, rc, id.agentNamespace()).orElseThrow();
                var store = new SessionSandboxStateStore(states, id.agentNamespace());
                var newer = client.deserializeState(store.load(key).orElseThrow(), spec);
                newer.setWorkspaceProjectionHash("another-node");
                String saved = client.serializeState(newer); store.save(key, saved);
                String snapshotVersion = records.get(List.of("liteflow", id.namespace(), "sandbox-snapshots-v1"), first.getState().getSessionId()).value().get("version").toString();
                clock.set(Duration.ofHours(1).toNanos()); registry.evictIdle();
                assertEquals(saved, store.load(key).orElseThrow());
                assertEquals(snapshotVersion, records.get(List.of("liteflow", id.namespace(), "sandbox-snapshots-v1"), first.getState().getSessionId()).value().get("version"));
                assertTrue(events.stream().anyMatch(e -> e.startsWith("shutdown:")));
            }
        }
    }
}
