package com.yomahub.liteflow.agent.harness.component;

import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.conversation.AgentConversationService;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.harness.storage.StoredWorkspaceFilesystem;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.*;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.*;
import io.agentscope.core.model.*;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.remote.store.InMemoryStore;
import io.agentscope.harness.agent.memory.MemoryConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HarnessMemoryModeTest {
    @TempDir Path temp;

    @AfterEach void clearConfiguration() {
        LiteflowConfigGetter.clean();
    }

    @Test void neverSkipsExtractionButPreservesContextArchivesAndDisplayHistory() throws Exception {
        verifyTurns(HarnessMemoryFlushMode.NEVER, 0);
    }

    @Test void alwaysExtractsAfterEachTurn() throws Exception {
        verifyTurns(HarnessMemoryFlushMode.ALWAYS, 2);
    }

    @Test void throttledExtractsOnlyOnceWithinTheConfiguredInterval() throws Exception {
        verifyTurns(HarnessMemoryFlushMode.THROTTLED, 1);
    }

    @Test void throttledExtractionResumesAfterTheConfiguredGap() throws Exception {
        // The custom filesystem uses the SDK USER coordination scope. Advance its public
        // test clock seam instead of sleeping for an hour or relying on scheduling speed.
        String key = "memory-flush:USER:";
        io.agentscope.harness.agent.coordination.LocalPeriodicGate.seedLastClaimAtForTests(key, java.time.Instant.EPOCH);
        AgentConfig config = configure(HarnessMemoryFlushMode.THROTTLED);
        try (Component component = new Component(config)) {
            component.process();
            assertTrue(io.agentscope.harness.agent.memory.MemoryBackgroundTasks.awaitQuiescence(5,
                    java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, component.memory.calls.get());
            component.process();
            assertTrue(io.agentscope.harness.agent.memory.MemoryBackgroundTasks.awaitQuiescence(5,
                    java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1, component.memory.calls.get());
            io.agentscope.harness.agent.coordination.LocalPeriodicGate.seedLastClaimAtForTests(
                    key, java.time.Instant.now().minus(Duration.ofHours(2)));
            component.process();
            assertTrue(io.agentscope.harness.agent.memory.MemoryBackgroundTasks.awaitQuiescence(5,
                    java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(2, component.memory.calls.get());
            assertEquals(3, component.primary.calls.get());
        } finally {
            io.agentscope.harness.agent.coordination.LocalPeriodicGate.seedLastClaimAtForTests(key, java.time.Instant.EPOCH);
        }
    }

    @Test void unconfiguredPolicyDisablesPerTurnExtraction() throws Exception {
        verifyTurns(null, 0);
    }

    @Test void explicitPropertiesOverrideOnlyTheJavaFlushTrigger() {
        CountingModel model = new CountingModel("NO_REPLY");
        MemoryConfig custom = MemoryConfig.builder().model(model).flushPrompt("extract")
                .consolidationPrompt("Summarize within %d tokens and %d characters")
                .consolidationMaxTokens(700).consolidationMinGap(Duration.ofHours(6))
                .dailyFileRetentionDays(12).sessionRetentionDays(30)
                .flushTrigger(MemoryConfig.FlushTrigger.throttled(Duration.ofHours(1))).build();
        HarnessMemoryConfig properties = new HarnessMemoryConfig();
        assertEquals(HarnessMemoryFlushMode.NEVER, properties.getFlushMode());
        assertEquals(MemoryConfig.FlushMode.NEVER,
                HarnessMemoryConfigResolver.resolve(null, properties).flushTrigger().mode());
        properties.setFlushMode(HarnessMemoryFlushMode.NEVER);
        MemoryConfig resolved = HarnessMemoryConfigResolver.resolve(custom, properties);
        assertEquals(MemoryConfig.FlushMode.NEVER, resolved.flushTrigger().mode());
        assertSame(model, resolved.model());
        assertEquals(custom.flushPrompt(), resolved.flushPrompt());
        assertEquals(custom.consolidationPrompt(), resolved.consolidationPrompt());
        assertEquals(700, resolved.consolidationMaxTokens());
        assertEquals(Duration.ofHours(6), resolved.consolidationMinGap());
        assertEquals(12, resolved.dailyFileRetentionDays());
        assertEquals(30, resolved.sessionRetentionDays());
    }

    @Test void nullModeCannotFallBackToTheSdkAlwaysDefault() {
        HarnessMemoryConfig properties = new HarnessMemoryConfig();
        properties.setFlushMode(null);
        assertThrows(IllegalStateException.class, () -> HarnessMemoryConfigResolver.resolve(null, properties));
    }

    @Test void invalidThrottleIntervalFailsBeforeBuildingTheRuntime() {
        AgentConfig config = configure(HarnessMemoryFlushMode.THROTTLED);
        for (Duration invalid : new Duration[]{null, Duration.ZERO, Duration.ofSeconds(-1)}) {
            config.getHarness().getMemory().setFlushMinGap(invalid);
            try (Component component = new Component(config)) {
                RuntimeException failure = assertThrows(RuntimeException.class, component::process);
                assertTrue(failure.getMessage().contains("flush-min-gap"));
                assertEquals(0, component.primary.calls.get());
                assertNull(component.runtime);
            }
        }
    }

    private void verifyTurns(HarnessMemoryFlushMode mode, int expectedExtraCalls) throws Exception {
        AgentConfig config = configure(mode);
        try (Component component = new Component(config)) {
            component.process();
            assertTrue(io.agentscope.harness.agent.memory.MemoryBackgroundTasks.awaitQuiescence(
                    5, java.util.concurrent.TimeUnit.SECONDS));
            component.process();
            assertTrue(io.agentscope.harness.agent.memory.MemoryBackgroundTasks.awaitQuiescence(
                    5, java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(2, component.primary.calls.get());
            assertEquals(expectedExtraCalls, component.memory.calls.get());
            assertTrue(component.primary.inputs.get(1).stream()
                    .anyMatch(message -> message.getRole() == MsgRole.ASSISTANT), "second turn must see prior Agent context");
            var identity = new InvocationIdentityResolver(config.getApplicationName())
                    .resolve("conversation", "memory-mode-agent");
            var context = RuntimeContext.builder().userId(null).sessionId(identity.runtimeSessionId()).build();
            var filesystem = component.runtime.agent().getWorkspaceManager().getFilesystem();
            var archives = filesystem.glob(context, "**/*.log.jsonl", "agents");
            assertFalse(archives.matches().isEmpty(), "NEVER must retain the Harness archive hook");
            String archive = filesystem.read(context, archives.matches().get(0).path(), 0, 0).fileData().content();
            assertTrue(archive.contains("hello"));
            assertTrue(archive.contains("answer"));
            try (var history = new AgentConversationService(config, component.states)) {
                assertEquals(4, history.messages("conversation", 0, 20).items().size());
            }
        }
    }

    private AgentConfig configure(HarnessMemoryFlushMode mode) {
        AgentConfig config = new AgentConfig();
        config.setApplicationName("memory-mode-" + UUID.randomUUID());
        config.setExecutionTimeout(Duration.ofSeconds(10));
        config.getHarness().getLocal().setWorkspaceRoot(temp.toString());
        config.getSessionStore().setJsonWorkspaceRoot(temp.toString());
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.CUSTOM);
        if (mode != null) {
            config.getHarness().getMemory().setFlushMode(mode);
        }
        config.getHarness().getMemory().setFlushMinGap(Duration.ofHours(1));
        config.getSkills().setEnabled(false);
        config.setConversationHistoryEnabled(true);
        LiteflowConfig liteflow = new LiteflowConfig();
        liteflow.setAgent(config);
        LiteflowConfigGetter.setLiteflowConfig(liteflow);
        return config;
    }

    private static final class Component extends HarnessAgentComponent {
        private final Slot slot = new Slot();
        private final InMemoryAgentStateStore states = new InMemoryAgentStateStore();
        private final InMemoryStore files = new InMemoryStore();
        private final CountingModel primary = new CountingModel("answer");
        private final CountingModel memory = new CountingModel("NO_REPLY");
        private final AgentConfig config;
        private HarnessAgentRuntime runtime;

        Component(AgentConfig config) {
            this.config = config;
            setNodeId("memory-mode-agent");
            slot.setConversationId("conversation");
            slot.setChainId("memory-test");
            slot.putRequestId(UUID.randomUUID().toString());
        }
        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("test model"); }
        @Override protected Model buildModel() { return primary; }
        @Override protected String systemPrompt() { return "Reply briefly"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "hello"; }
        @Override protected MemoryConfig memoryConfig() { return MemoryConfig.builder().model(memory).build(); }
        @Override protected AgentStateStoreResolver stateStoreResolver() { return ignored -> new ResolvedAgentStateStore(states, false); }
        @Override protected HarnessFilesystemConfigurer filesystemConfigurer() {
            return (builder, context) -> builder.abstractFilesystem(new StoredWorkspaceFilesystem(files,
                    rc -> List.of(config.getApplicationName(), rc.getUserId() == null ? "internal" : rc.getUserId(),
                            rc.getSessionId() == null ? "internal" : rc.getSessionId()), context.workspaceRoot()));
        }
        @Override protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext context) {
            runtime = super.buildRuntime(context);
            return runtime;
        }
        @Override protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return builder.disableSubagents().disableCompaction().disableToolResultEviction()
                    .disableWorkspaceContext().disableAtPathExpansion().disableDefaultWorkspaceSkills()
                    .disableDynamicSkills().disableToolsConfig().disableFilesystemTools().disableShellTool();
        }
    }

    private static final class CountingModel implements Model {
        private final String reply;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<List<Msg>> inputs = new ArrayList<>();
        CountingModel(String reply) { this.reply = reply; }
        @Override public String getModelName() { return "memory-mode-test"; }
        @Override public Flux<ChatResponse> stream(List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            calls.incrementAndGet();
            inputs.add(List.copyOf(messages));
            return Flux.just(ChatResponse.builder().content(List.of(TextBlock.builder().text(reply).build()))
                    .finishReason("stop").build());
        }
    }
}
