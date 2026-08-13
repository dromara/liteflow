package com.yomahub.liteflow.springboot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.component.AbstractAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.DockerSandboxConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import com.yomahub.liteflow.property.agent.AgentStateStoreFailurePolicy;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import com.yomahub.liteflow.property.agent.DistributedCoordinationMode;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.property.agent.WorkspaceBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Mono;

import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentPropertyBindingTest {

    private static final String PREFIX = "liteflow.agent.";
    private static final Set<String> LEGACY_MEMORY_KEYS = Set.of(
            PREFIX + "session.memory.mode",
            PREFIX + "session.memory.load-on-first-use",
            PREFIX + "session.memory.save-after-call",
            PREFIX + "session.memory.save-on-error",
            PREFIX + "session.memory.redis.bean-name",
            PREFIX + "session.memory.redis.client-type",
            PREFIX + "session.memory.redis.key-prefix",
            PREFIX + "session.memory.mysql.data-source-bean-name",
            PREFIX + "session.memory.mysql.database-name",
            PREFIX + "session.memory.mysql.table-name",
            PREFIX + "session.memory.mysql.create-if-not-exist");

    private static final Set<String> EXPECTED_AGENT_KEYS = Set.of(
            PREFIX + "runtime.namespace",
            PREFIX + "runtime.default-user-id",
            PREFIX + "runtime.timeout",
            PREFIX + "state-store.type",
            PREFIX + "state-store.bean-name",
            PREFIX + "state-store.json-root",
            PREFIX + "state-store.failure-policy",
            PREFIX + "toolkit.parallel",
            PREFIX + "event.listener-failure-mode",
            PREFIX + "invocation-guard.mode",
            PREFIX + "invocation-guard.bean-name",
            PREFIX + "invocation-guard.acquire-timeout",
            PREFIX + "invocation-guard.lease-duration",
            PREFIX + "invocation-guard.coordination-mode",
            PREFIX + "invocation-guard.strict-distributed",
            PREFIX + "hitl.confirmation-timeout",
            PREFIX + "hitl.fail-on-denied-tool",
            PREFIX + "harness.filesystem-backend",
            PREFIX + "harness.trusted-local",
            PREFIX + "harness.docker.image",
            PREFIX + "harness.docker.workspace-root",
            PREFIX + "harness.docker.memory-size-bytes",
            PREFIX + "harness.docker.cpu-count",
            PREFIX + "harness.docker.network",
            PREFIX + "harness.docker.snapshot-root",
            PREFIX + "harness.docker.workspace-projection-enabled",
            PREFIX + "harness.docker.workspace-projection-roots",
            PREFIX + "workspace.backend",
            PREFIX + "workspace.trusted-local",
            PREFIX + "workspace.root",
            PREFIX + "workspace.auto-create",
            PREFIX + "workspace.cleanup-on-session-expire",
            PREFIX + "workspace.cleanup-on-jvm-shutdown",
            PREFIX + "workspace.max-file-bytes",
            PREFIX + "workspace.max-list-size",
            PREFIX + "shell.mode",
            PREFIX + "shell.whitelist",
            PREFIX + "shell.blacklist",
            PREFIX + "shell.timeout",
            PREFIX + "shell.max-output-bytes",
            PREFIX + "defaults.max-iterations",
            PREFIX + "logging.react-enabled",
            PREFIX + "skills.enabled",
            PREFIX + "skills.path",
            PREFIX + "skills.strict",
            PREFIX + "openai.api-key",
            PREFIX + "openai.base-url",
            PREFIX + "openai.extra",
            PREFIX + "anthropic.api-key",
            PREFIX + "anthropic.base-url",
            PREFIX + "anthropic.extra",
            PREFIX + "gemini.api-key",
            PREFIX + "gemini.base-url",
            PREFIX + "gemini.extra",
            PREFIX + "dashscope.api-key",
            PREFIX + "dashscope.base-url",
            PREFIX + "dashscope.extra",
            PREFIX + "openai-compatible",
            PREFIX + "anthropic-compatible",
            PREFIX + "session.memory.mode",
            PREFIX + "session.memory.load-on-first-use",
            PREFIX + "session.memory.save-after-call",
            PREFIX + "session.memory.save-on-error",
            PREFIX + "session.memory.redis.bean-name",
            PREFIX + "session.memory.redis.client-type",
            PREFIX + "session.memory.redis.key-prefix",
            PREFIX + "session.memory.mysql.data-source-bean-name",
            PREFIX + "session.memory.mysql.database-name",
            PREFIX + "session.memory.mysql.table-name",
            PREFIX + "session.memory.mysql.create-if-not-exist");

    @Test
    void bindsEveryAgentScope2PropertyUsingExactKebabCasePaths() {
        LiteflowProperty property = bind(Map.ofEntries(
                entry("runtime.namespace", "binding-test"),
                entry("runtime.default-user-id", "user-7"),
                entry("runtime.timeout", "17s"),
                entry("state-store.type", "BEAN"),
                entry("state-store.bean-name", "stateStoreBean"),
                entry("state-store.json-root", "/tmp/agent-state"),
                entry("state-store.failure-policy", "LOG_AND_CONTINUE"),
                entry("toolkit.parallel", "true"),
                entry("event.listener-failure-mode", "LOG_AND_CONTINUE"),
                entry("invocation-guard.mode", "BEAN"),
                entry("invocation-guard.bean-name", "guardBean"),
                entry("invocation-guard.acquire-timeout", "19s"),
                entry("invocation-guard.lease-duration", "23s"),
                entry("invocation-guard.coordination-mode", "DISTRIBUTED_GUARD"),
                entry("invocation-guard.strict-distributed", "false"),
                entry("hitl.confirmation-timeout", "29s"),
                entry("hitl.fail-on-denied-tool", "true"),
                entry("harness.filesystem-backend", "DOCKER"),
                entry("harness.trusted-local", "false"),
                entry("harness.docker.image", "alpine:3.20"),
                entry("harness.docker.workspace-root", "/workspace"),
                entry("harness.docker.memory-size-bytes", "268435456"),
                entry("harness.docker.cpu-count", "1"),
                entry("harness.docker.network", "none"),
                entry("harness.docker.snapshot-root", "./data/agent-snapshots"),
                entry("harness.docker.workspace-projection-enabled", "true"),
                entry("harness.docker.workspace-projection-roots[0]", "AGENTS.md"),
                entry("harness.docker.workspace-projection-roots[1]", "skills"),
                entry("harness.docker.workspace-projection-roots[2]", "subagents"),
                entry("harness.docker.workspace-projection-roots[3]", "knowledge"),
                entry("harness.docker.workspace-projection-roots[4]", ".skills-cache"),
                entry("workspace.backend", "REMOTE_FILESYSTEM"),
                entry("workspace.trusted-local", "true"),
                entry("workspace.root", "/tmp/workspace"),
                entry("workspace.auto-create", "false"),
                entry("workspace.cleanup-on-session-expire", "false"),
                entry("workspace.cleanup-on-jvm-shutdown", "true"),
                entry("workspace.max-file-bytes", "12345"),
                entry("workspace.max-list-size", "321"),
                entry("shell.mode", "BLACKLIST"),
                entry("shell.whitelist[0]", "printf"),
                entry("shell.blacklist[0]", "shutdown"),
                entry("shell.timeout", "31s"),
                entry("shell.max-output-bytes", "54321"),
                entry("defaults.max-iterations", "27"),
                entry("logging.react-enabled", "false"),
                entry("skills.enabled", "true"),
                entry("skills.path", "/tmp/skills"),
                entry("skills.strict", "false")));

        AgentConfig agent = property.getAgent();
        assertEquals("binding-test", agent.getRuntime().getNamespace());
        assertEquals("user-7", agent.getRuntime().getDefaultUserId());
        assertEquals(Duration.ofSeconds(17), agent.getRuntime().getTimeout());
        assertEquals(AgentStateStoreType.BEAN, agent.getStateStore().getType());
        assertEquals("stateStoreBean", agent.getStateStore().getBeanName());
        assertEquals("/tmp/agent-state", agent.getStateStore().getJsonRoot());
        assertEquals(AgentStateStoreFailurePolicy.LOG_AND_CONTINUE,
                agent.getStateStore().getFailurePolicy());
        assertTrue(agent.getToolkit().isParallel());
        assertEquals(AgentListenerFailureMode.LOG_AND_CONTINUE,
                agent.getEvent().getListenerFailureMode());
        assertEquals(AgentInvocationGuardMode.BEAN, agent.getInvocationGuard().getMode());
        assertEquals("guardBean", agent.getInvocationGuard().getBeanName());
        assertEquals(Duration.ofSeconds(19), agent.getInvocationGuard().getAcquireTimeout());
        assertEquals(Duration.ofSeconds(23), agent.getInvocationGuard().getLeaseDuration());
        assertEquals(DistributedCoordinationMode.DISTRIBUTED_GUARD,
                agent.getInvocationGuard().getCoordinationMode());
        assertFalse(agent.getInvocationGuard().isStrictDistributed());
        assertEquals(Duration.ofSeconds(29), agent.getHitl().getConfirmationTimeout());
        assertTrue(agent.getHitl().isFailOnDeniedTool());
        assertEquals(HarnessFilesystemBackend.DOCKER,
                agent.getHarness().getFilesystemBackend());
        assertFalse(agent.getHarness().isTrustedLocal());
        DockerSandboxConfig docker = agent.getHarness().getDocker();
        assertEquals("alpine:3.20", docker.getImage());
        assertEquals("/workspace", docker.getWorkspaceRoot());
        assertEquals(268435456L, docker.getMemorySizeBytes());
        assertEquals(1L, docker.getCpuCount());
        assertEquals("none", docker.getNetwork());
        assertEquals("./data/agent-snapshots", docker.getSnapshotRoot());
        assertTrue(docker.isWorkspaceProjectionEnabled());
        assertEquals(List.of("AGENTS.md", "skills", "subagents", "knowledge", ".skills-cache"),
                docker.getWorkspaceProjectionRoots());
        assertDoesNotThrow(agent.getHarness()::validate);
        assertEquals(WorkspaceBackend.REMOTE_FILESYSTEM, agent.getWorkspace().getBackend());
        assertTrue(agent.getWorkspace().isTrustedLocal());
        assertEquals("/tmp/workspace", agent.getWorkspace().getRoot());
        assertFalse(agent.getWorkspace().isAutoCreate());
        assertFalse(agent.getWorkspace().isCleanupOnSessionExpire());
        assertTrue(agent.getWorkspace().isCleanupOnJvmShutdown());
        assertEquals(12345L, agent.getWorkspace().getMaxFileBytes());
        assertEquals(321, agent.getWorkspace().getMaxListSize());
        assertEquals(ShellMode.BLACKLIST, agent.getShell().getMode());
        assertEquals(Set.of("printf"), Set.copyOf(agent.getShell().getWhitelist()));
        assertEquals(Set.of("shutdown"), Set.copyOf(agent.getShell().getBlacklist()));
        assertEquals(Duration.ofSeconds(31), agent.getShell().getTimeout());
        assertEquals(54321L, agent.getShell().getMaxOutputBytes());
        assertEquals(27, agent.getDefaults().getMaxIterations());
        assertFalse(agent.getLogging().isReactEnabled());
        assertTrue(agent.getSkills().isEnabled());
        assertEquals("/tmp/skills", agent.getSkills().getPath());
        assertFalse(agent.getSkills().isStrict());
    }

    @Test
    void legacySessionMemoryBindingFailsWithTheExplicitMigrationDiagnostic() {
        AgentConfig agent = bind(Map.of(
                PREFIX + "runtime.namespace", "legacy-test",
                PREFIX + "session.memory.mode", "JVM")).getAgent();

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, agent::validateForExecution);

        assertTrue(failure.getMessage().contains("session.memory -> state-store"));
    }

    @Test
    void boundGuardedLocalConfigurationFailsClosedWithoutExplicitTrust() {
        AgentConfig agent = bind(Map.of(
                PREFIX + "harness.filesystem-backend", "GUARDED_LOCAL",
                PREFIX + "harness.trusted-local", "false")).getAgent();

        IllegalStateException failure = assertThrows(
                IllegalStateException.class, agent.getHarness()::validate);

        assertTrue(failure.getMessage().contains("trusted-local"));
    }

    @Test
    void metadataContainsTheSymmetricAgentKeySetAndDeprecatesOnlyLegacyMemory() throws Exception {
        JsonNode root;
        try (InputStream stream = AgentPropertyBindingTest.class.getResourceAsStream(
                "/META-INF/additional-spring-configuration-metadata.json")) {
            root = new ObjectMapper().readTree(stream);
        }
        Map<String, JsonNode> agentProperties = StreamSupport
                .stream(root.path("properties").spliterator(), false)
                .filter(node -> node.path("name").asText().startsWith(PREFIX))
                .collect(Collectors.toMap(
                        node -> node.path("name").asText(),
                        node -> node,
                        (left, right) -> left,
                        LinkedHashMap::new));

        assertEquals(EXPECTED_AGENT_KEYS, agentProperties.keySet());
        Set<String> deprecated = agentProperties.entrySet().stream()
                .filter(entry -> entry.getValue().has("deprecation"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        assertEquals(LEGACY_MEMORY_KEYS, deprecated);
        for (String key : LEGACY_MEMORY_KEYS) {
            JsonNode deprecation = agentProperties.get(key).path("deprecation");
            assertEquals("liteflow.agent.state-store",
                    deprecation.path("replacement").asText());
            assertEquals("warning", deprecation.path("level").asText());
        }

        assertMetadata(agentProperties, "runtime.timeout", "java.time.Duration", "2m");
        assertMetadata(agentProperties, "state-store.type",
                AgentStateStoreType.class.getName(), "MEMORY");
        assertMetadata(agentProperties, "state-store.failure-policy",
                AgentStateStoreFailurePolicy.class.getName(), "FAIL_FAST");
        assertMetadata(agentProperties, "toolkit.parallel", "java.lang.Boolean", "false");
        assertMetadata(agentProperties, "event.listener-failure-mode",
                AgentListenerFailureMode.class.getName(), "FAIL_FAST");
        assertMetadata(agentProperties, "invocation-guard.mode",
                AgentInvocationGuardMode.class.getName(), "LOCAL");
        assertMetadata(agentProperties, "invocation-guard.coordination-mode",
                DistributedCoordinationMode.class.getName(), "NONE");
        assertMetadata(agentProperties, "hitl.confirmation-timeout",
                "java.time.Duration", "2m");
        assertMetadata(agentProperties, "harness.filesystem-backend",
                HarnessFilesystemBackend.class.getName(), "GUARDED_LOCAL");
        assertMetadata(agentProperties, "harness.trusted-local",
                "java.lang.Boolean", "false");
        assertMetadata(agentProperties, "harness.docker.image",
                "java.lang.String", "ubuntu:22.04");
        assertMetadata(agentProperties, "harness.docker.workspace-root",
                "java.lang.String", "/workspace");
        assertMetadata(agentProperties, "harness.docker.memory-size-bytes",
                "java.lang.Long", "536870912");
        assertMetadata(agentProperties, "harness.docker.cpu-count",
                "java.lang.Long", "1");
        assertMetadata(agentProperties, "harness.docker.network",
                "java.lang.String", "none");
        assertMetadata(agentProperties, "harness.docker.workspace-projection-enabled",
                "java.lang.Boolean", "true");
        assertMetadata(agentProperties, "workspace.backend",
                WorkspaceBackend.class.getName(), "GUARDED_LOCAL");
        assertMetadata(agentProperties, "shell.mode",
                ShellMode.class.getName(), "DISABLED");
        assertMetadata(agentProperties, "defaults.max-iterations",
                "java.lang.Integer", "50");
    }

    @Test
    void springContextCloseInvokesThePublicAgentComponentCloseExactlyOnce() throws Exception {
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace("spring-lifecycle-test");
        LiteflowConfig liteflowConfig = new LiteflowConfig();
        liteflowConfig.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(liteflowConfig);

        GenericApplicationContext context = new GenericApplicationContext();
        SpringLifecycleComponent component = new SpringLifecycleComponent();
        context.registerBean("agentComponent", SpringLifecycleComponent.class, () -> component);
        context.refresh();
        try {
            component.process();

            context.close();
            context.close();

            assertEquals(1, component.runtime.closeCount.get());
            assertThrows(IllegalStateException.class, component::process);
        } finally {
            context.close();
            LiteflowConfigGetter.clean();
        }
    }

    private static LiteflowProperty bind(Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("test", properties));
        return Binder.get(environment)
                .bind("liteflow", Bindable.of(LiteflowProperty.class))
                .orElseThrow(() -> new AssertionError("liteflow properties were not bound"));
    }

    private static Map.Entry<String, Object> entry(String suffix, Object value) {
        return Map.entry(PREFIX + suffix, value);
    }

    private static void assertMetadata(
            Map<String, JsonNode> properties, String suffix, String type, String defaultValue) {
        JsonNode property = properties.get(PREFIX + suffix);
        assertEquals(type, property.path("type").asText(), suffix);
        assertEquals(defaultValue, property.path("defaultValue").asText(), suffix);
    }

    private static final class SpringLifecycleComponent
            extends AbstractAgentComponent<SpringLifecycleRuntime> {
        private final Slot slot = new Slot();
        private final SpringLifecycleRuntime runtime = new SpringLifecycleRuntime();

        private SpringLifecycleComponent() {
            slot.setChainId("spring-lifecycle-chain");
            slot.setConversationId("spring-lifecycle-conversation");
            slot.putRequestId("spring-lifecycle-request");
            setNodeId("spring-lifecycle-agent");
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected SpringLifecycleRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            return runtime;
        }

        @Override
        protected Mono<Msg> invokeRuntime(
                SpringLifecycleRuntime runtime,
                List<Msg> input,
                AgentOutputSpec output,
                RuntimeContext runtimeContext,
                LiteFlowAgentContext liteflowContext) {
            return Mono.just(AssistantMessage.builder().textContent("done").build());
        }

        @Override
        protected String systemPrompt() {
            return "spring lifecycle";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }
    }

    private static final class SpringLifecycleRuntime implements AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }
}
