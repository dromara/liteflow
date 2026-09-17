package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.IsolationScope;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.WorkspaceSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static com.yomahub.liteflow.agent.harness.sandbox.AgentSandboxStatus.State.*;
import static org.junit.jupiter.api.Assertions.*;

class AgentSandboxStatusServiceTest {
    @Test
    void observesBusyIdleEvictedAndRestoredWithoutTakingTheWorkspaceLease() throws Exception {
        var config = new AgentConfig();
        var identity = new InvocationIdentityResolver(UUID.randomUUID().toString()).resolve("chat", "agent");
        var guard = new AgentInvocationGuardResolver().resolve(config);
        var events = new ArrayList<String>();
        var client = new FakeSandboxClient(events);
        var clock = new AtomicLong();
        var actual = new AtomicReference<>(RUNNING);
        var service = new AgentSandboxStatusService(identity.namespace(), id ->
                new DockerContainerInspector.Inspection(actual.get(), id, "container", "test:latest", null));
        var workspace = new WorkspaceSpec();
        workspace.setRoot("/workspace");
        var template = SandboxContext.builder().client(client).workspaceSpec(workspace)
                .clientOptions(new DockerSandboxClientOptions()).snapshotSpec(new InMemorySandboxSnapshot(events))
                .isolationScope(IsolationScope.SESSION).build();
        assertEquals(NOT_ALLOCATED, service.getStatus("chat").state());
        assertEquals(0, client.createAttempts());
        try (var registry = new SessionSandboxRegistry(template, new InMemoryAgentStateStore(), "agent", guard,
                config.getHarness().getDocker(), clock::get, false)) {
            for (String containerId : new String[]{"first", "restored"}) {
                var context = RuntimeContext.builder().sessionId(identity.runtimeSessionId()).build();
                try (var lease = guard.acquire(AgentInvocationKey.workspace(identity), Duration.ZERO)) {
                    registry.execute(identity, context, () -> Mono.fromCallable(() -> {
                        var sandbox = context.get(SandboxContext.class).getExternalSandbox();
                        assertEquals(STARTING, service.getStatus("chat").state());
                        sandbox.start();
                        ((DockerSandboxState) sandbox.getState()).setContainerId(containerId);
                        var status = CompletableFuture.supplyAsync(() -> service.getStatus("chat"))
                                .get(1, TimeUnit.SECONDS);
                        assertEquals(RUNNING, status.state());
                        assertTrue(status.busy());
                        assertEquals(containerId, status.containerId());
                        assertNotNull(status.lastActiveAt());
                        return true;
                    })).block(Duration.ofSeconds(2));
                }
                var idle = service.getStatus("chat");
                assertEquals(RUNNING, idle.state());
                assertFalse(idle.busy());
                assertEquals("test:latest", idle.image());
                assertEquals("PROCESS_LOCAL", idle.scope());
                actual.set(STOPPED);
                assertEquals(STOPPED, service.getStatus("chat").state());
                actual.set(NOT_FOUND);
                assertEquals(NOT_FOUND, service.getStatus("chat").state());
                actual.set(RUNNING);
                clock.addAndGet(Duration.ofMinutes(11).toNanos());
                registry.evictIdle();
                var released = service.getStatus("chat");
                assertEquals(NOT_ALLOCATED, released.state());
                assertNull(released.containerId());
            }
        }
        assertEquals(NOT_ALLOCATED, service.getStatus("chat").state());
    }

    @Test
    void queriesAreIsolatedAndDoNotInventADockerObservationFromCachedMetadata() {
        String namespace = UUID.randomUUID().toString();
        var key = AgentInvocationKey.workspace(namespace, "chat");
        AgentSandboxStatusService.Source source = () ->
                new AgentSandboxStatusService.Snapshot("container", "cached", "configured", false, 123L);
        var service = new AgentSandboxStatusService(namespace, id -> DockerContainerInspector.Inspection.unknown("offline"));
        AgentSandboxStatusService.register(key, source);
        try {
            assertEquals(UNKNOWN, service.getStatus("chat").state());
            assertEquals(NOT_ALLOCATED, service.getStatus("other-chat").state());
            assertEquals(NOT_ALLOCATED, service.getStatus("other").state());
            assertEquals(NOT_ALLOCATED, new AgentSandboxStatusService("other").getStatus("chat").state());
            var concurrentEviction = new AgentSandboxStatusService(namespace, id -> {
                AgentSandboxStatusService.unregister(key, source);
                return new DockerContainerInspector.Inspection(RUNNING, id, "old", "old", null);
            });
            assertEquals(NOT_ALLOCATED, concurrentEviction.getStatus("chat").state());
        } finally {
            AgentSandboxStatusService.unregister(key, source);
        }
    }

    @Test
    void parsesActualDockerStatesAndDistinguishesRemovalFromDaemonFailure() {
        for (String value : new String[]{"running", "paused", "restarting", "removing", "dead", "created", "exited"}) {
            var result = DockerContainerInspector.parse(0, "{\"id\":\"abc\",\"name\":\"/sandbox\","
                    + "\"image\":\"test:latest\",\"status\":\"" + value + "\"}", "");
            assertEquals(value.equals("exited") ? STOPPED : AgentSandboxStatus.State.valueOf(value.toUpperCase(java.util.Locale.ROOT)), result.state());
            assertEquals("sandbox", result.name());
        }
        assertEquals(NOT_FOUND, DockerContainerInspector.parse(1, "", "Error: No such object: abc").state());
        assertEquals(UNKNOWN, DockerContainerInspector.parse(1, "", "Cannot connect to the Docker daemon").state());
        assertEquals(UNKNOWN, DockerContainerInspector.parse(1, "", "permission denied").state());
        assertEquals(UNKNOWN, DockerContainerInspector.parse(0, "not json", "").state());
        assertEquals(UNKNOWN, DockerContainerInspector.parse(0, "{}", "").state());
    }
}
