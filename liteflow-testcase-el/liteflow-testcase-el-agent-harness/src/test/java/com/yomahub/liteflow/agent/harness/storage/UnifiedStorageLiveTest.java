package com.yomahub.liteflow.agent.harness.storage;

import com.yomahub.liteflow.agent.context.*;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.guard.*;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.*;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import reactor.core.publisher.Flux;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in integration against dedicated MySQL/Redis services and a real Docker sandbox. */
@EnabledIfEnvironmentVariable(named = "LITEFLOW_TEST_SHARED_STORAGE", matches = "true")
class UnifiedStorageLiveTest {
    @Test void mysqlAndRedisPersistEverythingAcrossComponentRecreation() throws Exception {
        for (AgentSessionStoreType type : List.of(AgentSessionStoreType.MYSQL, AgentSessionStoreType.REDIS)) {
            AgentConfig config = configure(type);
            Path firstStaging;
            try (var first = new Component(false)) {
                first.process(); first.process();
                assertEquals(first.containerIds.get(0), first.containerIds.get(1), "consecutive calls should reuse Docker");
                firstStaging = first.runtime.agent().getWorkspaceManager().getWorkspace();
                try (var files = Files.walk(firstStaging)) {
                    assertTrue(files.noneMatch(p -> ManagedSandboxFilesystem.isManaged(firstStaging.relativize(p).toString())));
                }
            }
            assertFalse(Files.exists(firstStaging), "only disposable static staging may be local");
            try (var second = new Component(true)) {
                second.process();
                assertTrue(second.sawPriorConversation, "Agent context must survive a component restart");
                try (var storage = HarnessStorage.open(config.getSessionStore())) {
                    var id = new InvocationIdentityResolver(config.getApplicationName()).resolve("conversation", "storage-test");
                    var rc = RuntimeContext.builder().userId(null).sessionId(id.runtimeSessionId()).build();
                    var files = new ManagedSandboxFilesystem(storage.store(), config.getApplicationName(),
                            id.agentNamespace(), firstStaging, "/workspace");
                    List<String> ns = List.of("contract", config.getApplicationName());
                    assertTrue(storage.store().putIfVersion(ns, "record", Map.of("array", List.of()), 0));
                    assertFalse(storage.store().putIfVersion(ns, "record", Map.of("lost", true), 0));
                    assertEquals(List.of(), storage.store().get(ns, "record").value().get("array"));
                    assertTrue(storage.store().putIfVersion(ns, "record", Map.of("next", true), 1));
                    assertEquals("prefers Chinese", files.read(rc, "MEMORY.md", 0, 0).fileData().content());
                    assertFalse(files.glob(rc, "**/*.log.jsonl", "agents").matches().isEmpty(), "Harness archive must be remote");
                    assertFalse(storage.store().search(List.of("liteflow", config.getApplicationName(), "sandbox-snapshots-v1"), 100, 0).isEmpty());
                }
                try (var conversations = AgentConversationService.open(config)) {
                    assertFalse(conversations.messages("conversation", 0, 100).items().isEmpty());
                }
            } finally { LiteflowConfigGetter.clean(); }
            System.out.println("Unified " + type + " storage: context, history, memory, archive and Docker recovery passed");
        }
    }
    @Test void independentInstancesCoordinateTheSameConversation() {
        for (AgentSessionStoreType type : List.of(AgentSessionStoreType.MYSQL, AgentSessionStoreType.REDIS)) {
            AgentConfig config = configure(type);
            config.getInvocationGuard().setLeaseDuration(Duration.ofMillis(600));
            var key = AgentInvocationKey.workspace(config.getApplicationName(), "conversation");
            try (var first = new AgentInvocationGuardResolver().resolve(config);
                 var second = new AgentInvocationGuardResolver().resolve(config)) {
                try (var held = first.acquire(key, Duration.ZERO)) {
                    if (type == AgentSessionStoreType.REDIS) {
                        try { Thread.sleep(1000); } catch (InterruptedException failure) { throw new RuntimeException(failure); }
                    }
                    assertThrows(RuntimeException.class, () -> second.acquire(key, Duration.ZERO));
                }
                try (var acquired = second.acquire(key, Duration.ZERO)) { assertEquals(key, acquired.key()); }
            } finally { LiteflowConfigGetter.clean(); }
        }
    }
    private AgentConfig configure(AgentSessionStoreType type) {
        AgentConfig config = new AgentConfig();
        config.setApplicationName("unified-test-" + UUID.randomUUID());
        config.setExecutionTimeout(Duration.ofSeconds(60));
        config.setConversationHistoryEnabled(true);
        config.getSessionStore().setType(type);
        config.getSessionStore().getMysql().setJdbcUrl(System.getenv().getOrDefault("LITEFLOW_TEST_MYSQL_URL", "jdbc:mysql://127.0.0.1:13316/liteflow_storage_test?allowPublicKeyRetrieval=true&useSSL=false"));
        config.getSessionStore().getMysql().setUsername("root");
        config.getSessionStore().getMysql().setPassword("");
        config.getSessionStore().getMysql().setDatabaseName("liteflow_storage_test");
        config.getSessionStore().getMysql().setTableName("agent_sessions");
        config.getSessionStore().getMysql().setCreateIfNotExist(true);
        config.getSessionStore().getRedis().setUri(System.getenv().getOrDefault("LITEFLOW_TEST_REDIS_URI", "redis://127.0.0.1:16386"));
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        config.getHarness().getDocker().setImage(System.getenv().getOrDefault("LITEFLOW_SANDBOX_TEST_IMAGE", "liteflow-agent-sandbox:node22"));
        config.getHarness().getDocker().setLifecycle(DockerSandboxLifecycle.SESSION_IDLE);
        config.getHarness().getDocker().setWorkspaceProjectionEnabled(true);
        config.getSkills().setEnabled(true);
        config.getSkills().setPath("classpath:storage-skills");
        LiteflowConfig liteflow = new LiteflowConfig(); liteflow.setAgent(config); LiteflowConfigGetter.setLiteflowConfig(liteflow);
        return config;
    }
    private static final class Component extends HarnessAgentComponent {
        private final Slot slot = new Slot();
        private final boolean restored;
        private HarnessAgentRuntime runtime;
        private final List<String> containerIds = new ArrayList<>();
        private boolean sawPriorConversation;
        Component(boolean restored) {
            this.restored = restored; setNodeId("storage-test");
            slot.setConversationId("conversation"); slot.setChainId("chain"); slot.putRequestId(UUID.randomUUID().toString());
        }
        @Override public Slot getSlot() { return slot; }
        @Override protected String systemPrompt() { return "Reply briefly"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "question"; }
        @Override protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext context) {
            runtime = super.buildRuntime(context); return runtime;
        }
        @Override protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return builder.disableSubagents().disableCompaction().disableToolResultEviction()
                    .disableAtPathExpansion().disableDefaultWorkspaceSkills().disableDynamicSkills().disableToolsConfig();
        }
        @Override protected com.yomahub.liteflow.agent.model.ModelSpec<?> model() { throw new AssertionError("test model"); }
        @Override protected Model buildModel() {
            return new Model() {
                public String getModelName() { return "storage-regression"; }
                public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
                    try {
                        var fs = (ManagedSandboxFilesystem) runtime.agent().getWorkspaceManager().getFilesystem();
                        var sandbox = fs.getSandbox();
                        var id = new InvocationIdentityResolver(agentConfig().getApplicationName()).resolve("conversation", "storage-test");
                        var rc = RuntimeContext.builder().userId(null).sessionId(id.runtimeSessionId()).build();
                        containerIds.add(((DockerSandboxState) sandbox.getState()).getContainerId());
                        assertEquals(0, sandbox.exec(rc, "test ! -e /workspace/memory && test ! -e /workspace/MEMORY.md && test ! -e /workspace/agents", 5).exitCode());
                        assertTrue(sandbox.exec(rc, "find /workspace/.skills-cache -name hello.sh -exec sh {} +", 5).stdout().contains("skill-ready"),
                                "repository skills must be staged before container startup");
                        if (restored) {
                            assertEquals("business-file", sandbox.exec(rc, "cat /workspace/result.txt", 5).stdout());
                            assertEquals("prefers Chinese", fs.read(rc, "MEMORY.md", 0, 0).fileData().content());
                            sawPriorConversation |= messages.stream().filter(m -> m instanceof UserMessage).count() > 1;
                        } else {
                            sandbox.exec(rc, "printf business-file > /workspace/result.txt", 5);
                            fs.uploadFiles(rc, List.of(Map.entry("MEMORY.md", "prefers Chinese".getBytes())));
                        }
                        return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder().text("ready").build())).finishReason("stop").build());
                    } catch (Exception failure) { return Flux.error(failure); }
                }
            };
        }
    }
}
