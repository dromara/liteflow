package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuardResolver;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.Sandbox;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/** Real Docker and snapshot IO, deterministic in-process model; opt-in, no model credentials. */
@EnabledIfEnvironmentVariable(named = "LITEFLOW_TEST_DOCKER", matches = "true")
class SessionSandboxDockerTest {
    @TempDir Path temp;

    @Test
    void realHarnessReusesContainerThenRestoresSnapshotAfterIdleEviction() throws Exception {
        AgentConfig config = new AgentConfig();
        config.getHarness().getDocker().setImage(System.getenv().getOrDefault(
                "LITEFLOW_SANDBOX_TEST_IMAGE", "liteflow-agent-sandbox:node22"));
        config.getHarness().getDocker().setSnapshotRoot(temp.resolve("snapshots").toString());
        config.getHarness().getDocker().setWorkspaceProjectionEnabled(true);
        Path reference = java.nio.file.Files.createDirectories(temp.resolve("workspace/knowledge")).resolve("reference.txt");
        java.nio.file.Files.writeString(reference, "host-static-reference");
        var guard = new AgentInvocationGuardResolver().resolve(config);
        var identity = new InvocationIdentityResolver("docker-regression-" + UUID.randomUUID())
                .resolve("session", "agent");
        var statusService = new AgentSandboxStatusService(identity.namespace());
        assertEquals(AgentSandboxStatus.State.NOT_ALLOCATED, statusService.getStatus("session").state());
        var store = new InMemoryAgentStateStore();
        var configurer = new DockerSandboxConfigurer();
        var builder = HarnessAgent.builder().name("sandbox-regression").agentId(identity.agentNamespace())
                .model(new FixedModel()).stateStore(store).workspace(temp.resolve("workspace"))
                .disableSubagents().disableCompaction().disableToolResultEviction()
                .disableMemoryTools().disableMemoryHooks().disableWorkspaceContext()
                .disableAtPathExpansion().disableDefaultWorkspaceSkills().disableDynamicSkills()
                .disableToolsConfig();
        configurer.configure(builder, new HarnessFilesystemContext(temp.resolve("workspace"), Duration.ofSeconds(10), config));
        AtomicLong clock = new AtomicLong();
        String firstId;
        String restoredId;
        long started = System.nanoTime();
        try (var agent = builder.build();
             var registry = new SessionSandboxRegistry(configurer.sandboxContext(), store,
                     identity.agentNamespace(), guard, config.getHarness().getDocker(), clock::get, false)) {
            Sandbox first;
            try (var lease = guard.acquire(AgentInvocationKey.workspace(identity), Duration.ofSeconds(2))) {
                first = turn(registry, agent, identity);
                assertEquals("host-static-reference", first.exec(context(identity),
                        "cat /workspace/knowledge/reference.txt", 5).stdout());
                assertEquals(0, first.exec(context(identity),
                        "printf container-change > /workspace/knowledge/reference.txt", 5).exitCode());
                assertEquals("host-static-reference", java.nio.file.Files.readString(reference),
                        "static workspace projection must copy files, not mount the host source");
                first.stop();
                assertFalse(first.isRunning(), "SDK lifecycle flag is cleared by checkpointing");
                var idleStatus = statusService.getStatus("session");
                assertEquals(AgentSandboxStatus.State.RUNNING, idleStatus.state(), "Docker still runs after checkpointing");
                assertFalse(idleStatus.busy());
                assertNotNull(idleStatus.containerId());
                first.start();
                first.exec(context(identity), "printf retained > /workspace/answer.txt; printf transient > /tmp/turn-marker", 5);
                Sandbox second = turn(registry, agent, identity);
                assertSame(first, second);
                firstId = ((DockerSandboxState) first.getState()).getContainerId();
                assertEquals(firstId, ((DockerSandboxState) second.getState()).getContainerId());
                assertEquals("transient", second.exec(context(identity), "cat /tmp/turn-marker", 5).stdout());
            }
            clock.set(Duration.ofMinutes(11).toNanos());
            long evictionStarted = System.nanoTime();
            registry.evictIdle();
            assertEquals(AgentSandboxStatus.State.NOT_ALLOCATED, statusService.getStatus("session").state());
            assertTrue(Duration.ofNanos(System.nanoTime() - evictionStarted).compareTo(Duration.ofSeconds(10)) < 0,
                    "idle cleanup should not wait for the old 30-second Docker stop timeout");
            try (var lease = guard.acquire(AgentInvocationKey.workspace(identity), Duration.ofSeconds(2))) {
                Sandbox restored = turn(registry, agent, identity);
                restoredId = ((DockerSandboxState) restored.getState()).getContainerId();
                assertEquals(restoredId, statusService.getStatus("session").containerId());
                assertNotEquals(firstId, restoredId);
                assertEquals("retained", restored.exec(context(identity), "cat /workspace/answer.txt", 5).stdout());
                assertEquals("absent", restored.exec(context(identity), "test ! -e /tmp/turn-marker && printf absent", 5).stdout());
            }
        }
        assertContainerRemoved(firstId);
        assertContainerRemoved(restoredId);
        assertEquals(AgentSandboxStatus.State.NOT_ALLOCATED, statusService.getStatus("session").state());
        System.out.println("Real Docker reuse + snapshot restore passed in "
                + Duration.ofNanos(System.nanoTime() - started).toMillis() + "ms");
    }

    private Sandbox turn(SessionSandboxRegistry registry, HarnessAgent agent, AgentInvocationIdentity identity) {
        RuntimeContext context = context(identity);
        AtomicReference<Sandbox> sandbox = new AtomicReference<>();
        registry.execute(identity, context, () -> {
            sandbox.set(context.get(SandboxContext.class).getExternalSandbox());
            return agent.call(List.of(new UserMessage("reply")), context).doOnNext(reply -> {
                var status = new AgentSandboxStatusService(identity.namespace()).getStatus("session");
                assertEquals(AgentSandboxStatus.State.RUNNING, status.state());
                assertTrue(status.busy());
            });
        }).block(Duration.ofSeconds(30));
        return sandbox.get();
    }

    private RuntimeContext context(AgentInvocationIdentity identity) {
        return RuntimeContext.builder().userId(null).sessionId(identity.runtimeSessionId()).build();
    }

    private void assertContainerRemoved(String id) throws Exception {
        Process inspect = new ProcessBuilder("docker", "inspect", id)
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        assertTrue(inspect.waitFor(5, java.util.concurrent.TimeUnit.SECONDS));
        assertNotEquals(0, inspect.exitValue());
    }

    private static final class FixedModel implements Model {
        public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder().text("ready").build()))
                    .finishReason("stop").build());
        }
        public String getModelName() { return "deterministic-sandbox-test"; }
    }
}
