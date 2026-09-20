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
import com.yomahub.liteflow.property.agent.DockerSandboxLifecycle;
import com.yomahub.liteflow.property.agent.HarnessMemoryFlushMode;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import com.yomahub.liteflow.property.agent.AgentSessionStoreFailurePolicy;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import com.yomahub.liteflow.property.agent.DockerNetworkMode;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.Msg;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import com.yomahub.liteflow.springboot.config.LiteflowPropertyAutoConfiguration;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.ClassUtils;
import org.springframework.context.support.GenericApplicationContext;
import reactor.core.publisher.Mono;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.InputStream;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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

    @Test
    void bindsEveryAgentScope2PropertyUsingExactKebabCasePaths() {
        LiteflowProperty property = bind(Map.ofEntries(
                entry("application-name", "binding-test"),
                entry("execution-timeout", "17s"),
                entry("session-store.type", "REDIS"),
                entry("session-store.json-root", "/tmp/agent-state"),
                entry("session-store.json-workspace-root", "/tmp/agent-records"),
                entry("session-store.failure-policy", "LOG_AND_CONTINUE"),
                entry("session-store.redis.uri", "redis://localhost:6379"),
                entry("session-store.redis.client-bean-name", ""),
                entry("session-store.redis.key-prefix", "liteflow:agent:state:"),
                entry("session-store.mysql.jdbc-url", "jdbc:mysql://localhost:3306/liteflow"),
                entry("session-store.mysql.username", "lf"),
                entry("session-store.mysql.password", "secret"),
                entry("session-store.mysql.database-name", "liteflow"),
                entry("session-store.mysql.table-name", "agent_state"),
                entry("session-store.mysql.create-if-not-exist", "true"),
                entry("toolkit.parallel", "true"),
                entry("event.listener-failure-mode", "LOG_AND_CONTINUE"),
                entry("invocation-guard.mode", "BEAN"),
                entry("invocation-guard.bean-name", "guardBean"),
                entry("invocation-guard.acquire-timeout", "19s"),
                entry("invocation-guard.lease-duration", "23s"),
                entry("hitl.confirmation-timeout", "29s"),
                entry("hitl.fail-on-denied-tool", "true"),
                entry("harness.filesystem-backend", "DOCKER"),
                entry("harness.docker.image", "alpine:3.20"),
                entry("harness.docker.workspace-root", "/workspace"),
                entry("harness.docker.memory-size-bytes", "268435456"),
                entry("harness.docker.cpu-count", "1"),
                entry("harness.docker.network", "none"),
                entry("harness.docker.snapshot-root", "./data/agent-snapshots"),
                entry("harness.docker.lifecycle", "SESSION_IDLE"),
                entry("harness.docker.idle-timeout", "41s"),
                entry("harness.docker.eviction-interval", "7s"),
                entry("harness.docker.max-cached-sandboxes", "12"),
                entry("harness.docker.workspace-projection-enabled", "true"),
                entry("harness.docker.workspace-projection-roots[0]", "AGENTS.md"),
                entry("harness.docker.workspace-projection-roots[1]", "skills"),
                entry("harness.docker.workspace-projection-roots[2]", "subagents"),
                entry("harness.docker.workspace-projection-roots[3]", "knowledge"),
                entry("harness.docker.workspace-projection-roots[4]", ".skills-cache"),
                entry("harness.local.workspace-root", "/tmp/workspace"),
                entry("harness.compaction-threshold", "0.75"),
                entry("harness.compaction-fallback-context-window", "262144"),
                entry("harness.compaction-fallback-threshold", "0.85"),
                entry("harness.memory.flush-mode", "THROTTLED"),
                entry("harness.memory.flush-min-gap", "43s"),
                entry("harness.shell.mode", "WHITELIST"),
                entry("harness.shell.whitelist[0]", "printf"),
                entry("harness.shell.timeout", "31s"),
                entry("max-iterations", "27"),
                entry("execution-log-enabled", "false"),
                entry("skills.enabled", "true"),
                entry("skills.path", "/tmp/skills"),
                entry("skills.strict", "false")));

        AgentConfig agent = property.getAgent();
        assertEquals("binding-test", agent.getApplicationName());
        assertEquals(Duration.ofSeconds(17), agent.getExecutionTimeout());
        assertEquals(AgentSessionStoreType.REDIS, agent.getSessionStore().getType());
        assertEquals("/tmp/agent-state", agent.getSessionStore().getJsonRoot());
        assertEquals(AgentSessionStoreFailurePolicy.LOG_AND_CONTINUE,
                agent.getSessionStore().getFailurePolicy());
        assertEquals("redis://localhost:6379", agent.getSessionStore().getRedis().getUri());
        assertEquals("liteflow:agent:state:", agent.getSessionStore().getRedis().getKeyPrefix());
        assertEquals("jdbc:mysql://localhost:3306/liteflow",
                agent.getSessionStore().getMysql().getJdbcUrl());
        assertEquals("lf", agent.getSessionStore().getMysql().getUsername());
        assertEquals("secret", agent.getSessionStore().getMysql().getPassword());
        assertEquals("liteflow", agent.getSessionStore().getMysql().getDatabaseName());
        assertEquals("agent_state", agent.getSessionStore().getMysql().getTableName());
        assertTrue(agent.getSessionStore().getMysql().isCreateIfNotExist());
        assertTrue(agent.getToolkit().isParallel());
        assertEquals(AgentListenerFailureMode.LOG_AND_CONTINUE,
                agent.getEvent().getListenerFailureMode());
        assertEquals(AgentInvocationGuardMode.BEAN, agent.getInvocationGuard().getMode());
        assertEquals("guardBean", agent.getInvocationGuard().getBeanName());
        assertEquals(Duration.ofSeconds(19), agent.getInvocationGuard().getAcquireTimeout());
        assertEquals(Duration.ofSeconds(23), agent.getInvocationGuard().getLeaseDuration());
        assertEquals(Duration.ofSeconds(29), agent.getHitl().getConfirmationTimeout());
        assertTrue(agent.getHitl().isFailOnDeniedTool());
        assertEquals(HarnessFilesystemBackend.DOCKER,
                agent.getHarness().getFilesystemBackend());
        DockerSandboxConfig docker = agent.getHarness().getDocker();
        assertEquals("alpine:3.20", docker.getImage());
        assertEquals("/workspace", docker.getWorkspaceRoot());
        assertEquals(268435456L, docker.getMemorySizeBytes());
        assertEquals(1L, docker.getCpuCount());
        assertEquals(DockerNetworkMode.NONE, docker.getNetwork());
        assertEquals("./data/agent-snapshots", docker.getSnapshotRoot());
        assertEquals(DockerSandboxLifecycle.SESSION_IDLE, docker.getLifecycle());
        assertEquals(Duration.ofSeconds(41), docker.getIdleTimeout());
        assertEquals(Duration.ofSeconds(7), docker.getEvictionInterval());
        assertEquals(12, docker.getMaxCachedSandboxes());
        assertTrue(docker.isWorkspaceProjectionEnabled());
        assertEquals(List.of("AGENTS.md", "skills", "subagents", "knowledge", ".skills-cache"),
                docker.getWorkspaceProjectionRoots());
        assertEquals(HarnessMemoryFlushMode.THROTTLED, agent.getHarness().getMemory().getFlushMode());
        assertEquals(Duration.ofSeconds(43), agent.getHarness().getMemory().getFlushMinGap());
        assertDoesNotThrow(agent.getHarness()::validate);
        assertEquals("/tmp/agent-records", agent.getSessionStore().getJsonWorkspaceRoot());
        assertEquals("/tmp/workspace", agent.getHarness().getLocal().getWorkspaceRoot());
        assertEquals(0.75, agent.getHarness().getCompactionThreshold());
        assertEquals(262144, agent.getHarness().getCompactionFallbackContextWindow());
        assertEquals(0.85, agent.getHarness().getCompactionFallbackThreshold());
        assertEquals(ShellMode.WHITELIST, agent.getHarness().getShell().getMode());
        assertEquals(Set.of("printf"), Set.copyOf(agent.getHarness().getShell().getWhitelist()));
        assertEquals(Duration.ofSeconds(31), agent.getHarness().getShell().getTimeout());
        assertEquals(27, agent.getMaxIterations());
        assertFalse(agent.isExecutionLogEnabled());
        assertTrue(agent.getSkills().isEnabled());
        assertEquals("/tmp/skills", agent.getSkills().getPath());
        assertFalse(agent.getSkills().isStrict());
    }

    @Test
    void applicationNameDefaultsToSpringApplicationNameWithoutAgentConfiguration() {
        new ApplicationContextRunner()
                .withUserConfiguration(LiteflowPropertyAutoConfiguration.class)
                .withPropertyValues("spring.application.name=orders-module")
                .run(context -> {
                    AgentConfig agent = context.getBean(LiteflowConfig.class).getAgent();
                    assertEquals("orders-module", agent.getApplicationName());
                    assertEquals(Duration.ofMinutes(10), agent.getExecutionTimeout());
                    assertEquals(100, agent.getMaxIterations());
                    assertEquals(Duration.ofMinutes(1), agent.getHarness().getShell().getTimeout());
                    assertTrue(agent.isExecutionLogEnabled());
                    assertDoesNotThrow(agent::validateForExecution);
                });
    }

    @Test
    void explicitAgentApplicationNameAndExecutionTimeoutOverrideDefaults() {
        new ApplicationContextRunner()
                .withUserConfiguration(LiteflowPropertyAutoConfiguration.class)
                .withPropertyValues("spring.application.name=orders-module",
                        "liteflow.agent.application-name=shared-orders",
                        "liteflow.agent.execution-timeout=45s",
                        "liteflow.agent.max-iterations=27",
                        "liteflow.agent.execution-log-enabled=false")
                .run(context -> {
                    AgentConfig agent = context.getBean(LiteflowConfig.class).getAgent();
                    assertEquals("shared-orders", agent.getApplicationName());
                    assertEquals(Duration.ofSeconds(45), agent.getExecutionTimeout());
                    assertEquals(27, agent.getMaxIterations());
                    assertFalse(agent.isExecutionLogEnabled());
                });
    }

    @Test
    void dockerNetworkAcceptsOnlySupportedModes() {
        assertEquals(DockerNetworkMode.BRIDGE,
                bind(Map.of(PREFIX + "harness.docker.network", "bridge"))
                        .getAgent().getHarness().getDocker().getNetwork());
        assertEquals(DockerNetworkMode.HOST,
                bind(Map.of(PREFIX + "harness.docker.network", "host"))
                        .getAgent().getHarness().getDocker().getNetwork());

        assertThrows(BindException.class,
                () -> bind(Map.of(PREFIX + "harness.docker.network", "custom-network")));
    }

    @Test
    void boundGuardedLocalConfigurationIsValidWithoutAnAdditionalTrustFlag() {
        AgentConfig agent = bind(Map.of(
                PREFIX + "harness.filesystem-backend", "GUARDED_LOCAL")).getAgent();
        assertDoesNotThrow(agent.getHarness()::validate);
    }

    @Test
    void metadataCoversEveryAgentPropertyWithMatchingTypes() throws Exception {
        JsonNode root;
        try (InputStream stream = AgentPropertyBindingTest.class.getResourceAsStream(
                "/META-INF/additional-spring-configuration-metadata.json")) {
            root = new ObjectMapper().readTree(stream);
        }
        Set<String> rawMapProperties = StreamSupport
                .stream(root.path("properties").spliterator(), false)
                .filter(node -> "java.util.Map".equals(node.path("type").asText()))
                .map(node -> node.path("name").asText())
                .collect(Collectors.toSet());
        assertEquals(Set.of(), rawMapProperties,
                "metadata Map types must declare key and value types");

        Map<String, JsonNode> agentProperties = StreamSupport
                .stream(root.path("properties").spliterator(), false)
                .filter(node -> node.path("name").asText().startsWith(PREFIX))
                .collect(Collectors.toMap(
                        node -> node.path("name").asText(),
                        node -> node,
                        (left, right) -> left,
                        LinkedHashMap::new));

        Map<String, String> expectedTypes = agentPropertyTypes(AgentConfig.class, PREFIX);
        assertEquals(expectedTypes.keySet(), agentProperties.keySet());
        expectedTypes.forEach((name, type) -> assertEquals(
                type, agentProperties.get(name).path("type").asText().replace(" ", ""), name));
        Set<String> deprecated = agentProperties.entrySet().stream()
                .filter(entry -> entry.getValue().has("deprecation"))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        assertEquals(Set.of(), deprecated, "no liteflow.agent.* key should be deprecated");

        assertMetadata(agentProperties, "execution-timeout", "java.time.Duration", "10m");
        assertMetadata(agentProperties, "session-store.type",
                AgentSessionStoreType.class.getName(), "JSON");
        assertMetadata(agentProperties, "session-store.failure-policy",
                AgentSessionStoreFailurePolicy.class.getName(), "FAIL_FAST");
        assertMetadata(agentProperties, "toolkit.parallel", "java.lang.Boolean", "false");
        assertMetadata(agentProperties, "event.listener-failure-mode",
                AgentListenerFailureMode.class.getName(), "FAIL_FAST");
        assertMetadata(agentProperties, "invocation-guard.mode",
                AgentInvocationGuardMode.class.getName(), new AgentConfig().getInvocationGuard().getMode().name());
        assertMetadata(agentProperties, "hitl.confirmation-timeout",
                "java.time.Duration", "2m");
        assertMetadata(agentProperties, "harness.filesystem-backend",
                HarnessFilesystemBackend.class.getName(), "GUARDED_LOCAL");
        assertMetadata(agentProperties, "harness.docker.image",
                "java.lang.String", "ubuntu:22.04");
        assertMetadata(agentProperties, "harness.docker.workspace-root",
                "java.lang.String", "/workspace");
        assertMetadata(agentProperties, "harness.docker.memory-size-bytes",
                "java.lang.Long", "536870912");
        assertMetadata(agentProperties, "harness.docker.cpu-count",
                "java.lang.Long", "1");
        assertMetadata(agentProperties, "harness.docker.lifecycle",
                DockerSandboxLifecycle.class.getName(), "PER_CALL");
        assertMetadata(agentProperties, "harness.docker.idle-timeout",
                "java.time.Duration", "10m");
        assertMetadata(agentProperties, "harness.docker.eviction-interval",
                "java.time.Duration", "30s");
        assertMetadata(agentProperties, "harness.docker.max-cached-sandboxes",
                "java.lang.Integer", "8");
        assertMetadata(agentProperties, "harness.memory.flush-mode",
                HarnessMemoryFlushMode.class.getName(), "NEVER");
        assertMetadata(agentProperties, "harness.memory.flush-min-gap",
                "java.time.Duration", "5m");
        assertMetadata(agentProperties, "harness.docker.network",
                DockerNetworkMode.class.getName(), "NONE");
        assertMetadata(agentProperties, "harness.docker.workspace-projection-enabled",
                "java.lang.Boolean", "true");
        assertMetadata(agentProperties, "harness.shell.mode",
                ShellMode.class.getName(), "WHITELIST");
        assertMetadata(agentProperties, "harness.shell.timeout",
                "java.time.Duration", "1m");
        assertMetadata(agentProperties, "max-iterations",
                "java.lang.Integer", "100");
        assertMetadata(agentProperties, "execution-log-enabled",
                "java.lang.Boolean", "true");
    }

    @Test
    void springContextCloseInvokesThePublicAgentComponentCloseExactlyOnce() throws Exception {
        AgentConfig agent = new AgentConfig();
        agent.setApplicationName("spring-lifecycle-test");
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

    private static Map<String, String> agentPropertyTypes(Class<?> beanType, String prefix)
            throws Exception {
        Map<String, String> types = new LinkedHashMap<>();
        for (PropertyDescriptor property : Introspector.getBeanInfo(beanType, Object.class)
                .getPropertyDescriptors()) {
            String name = prefix + property.getName()
                    .replaceAll("([a-z0-9])([A-Z])", "$1-$2").toLowerCase(Locale.ROOT);
            Class<?> type = property.getPropertyType();
            if (!type.isEnum() && type.getPackageName().equals(AgentConfig.class.getPackageName())) {
                types.putAll(agentPropertyTypes(type, name + "."));
            } else {
                String metadataType = type.isPrimitive()
                        ? ClassUtils.resolvePrimitiveIfNecessary(type).getName()
                        : property.getReadMethod().getGenericReturnType().getTypeName().replace(" ", "");
                types.put(name, metadataType);
            }
        }
        return types;
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
