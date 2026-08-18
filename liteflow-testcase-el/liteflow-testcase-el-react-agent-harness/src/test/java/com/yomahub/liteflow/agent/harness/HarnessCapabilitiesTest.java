package com.yomahub.liteflow.agent.harness;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.harness.sandbox.FakeSandboxClient;
import com.yomahub.liteflow.agent.harness.sandbox.SandboxSnapshotProvider;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolResultState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.middleware.ReasoningInput;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.tool.Tool;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileData;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
import io.agentscope.harness.agent.filesystem.model.FileInfo;
import io.agentscope.harness.agent.filesystem.model.FileUploadResponse;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.GrepResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ConversationCompactor;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.WorkspaceMode;
import io.agentscope.harness.agent.subagent.task.BackgroundTask;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.TaskRunSpec;
import io.agentscope.harness.agent.subagent.task.TaskStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessCapabilitiesTest {

    @TempDir
    Path tempDir;

    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        components.forEach(TestComponent::close);
        LiteflowConfigGetter.clean();
    }

    @Test
    void additionalContextFilesReachTheRealModelInDeclaredOrder() throws Exception {
        configureCustom("additional-context");
        RecordingFilesystem filesystem = new RecordingFilesystem(Map.of(
                "context/first.md", "FIRST-CONTEXT",
                "context/second.md", "SECOND-CONTEXT"));
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, filesystem);
        component.additionalContextFiles = List.of("context/first.md", "context/second.md");

        component.process();

        String prompt = model.systemText();
        assertTrue(prompt.contains("FIRST-CONTEXT"), prompt);
        assertTrue(prompt.contains("SECOND-CONTEXT"), prompt);
        assertTrue(prompt.indexOf("FIRST-CONTEXT") < prompt.indexOf("SECOND-CONTEXT"), prompt);
    }

    @ParameterizedTest
    @MethodSource("invalidAdditionalContextFiles")
    void invalidOrDuplicateAdditionalContextFilesFailBeforeModelInvocation(List<String> files)
            throws Exception {
        configureCustom("invalid-additional-context");
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.additionalContextFiles = files;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("additional context"), failure.getMessage());
        assertEquals(0, model.calls.get());
    }

    static Stream<List<String>> invalidAdditionalContextFiles() {
        return Stream.of(
                java.util.Arrays.asList("context.md", null),
                List.of("context.md", " "),
                List.of("context.md", "context.md"));
    }

    @Test
    void nullCapabilityHooksPreserveHarnessDefaults() throws Exception {
        configureCustom("null-defaults");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.inspectBuilder = builder -> {
            assertFalse(booleanField(builder, "disableCompaction"));
            assertFalse(booleanField(builder, "disableToolResultEviction"));
            MemoryConfig upstreamDefault = (MemoryConfig) field(builder, "memoryConfig");
            assertEquals(MemoryConfig.DEFAULT_SESSION_RETENTION_DAYS,
                    upstreamDefault.sessionRetentionDays());
            assertEquals(MemoryConfig.DEFAULT_DAILY_FILE_RETENTION_DAYS,
                    upstreamDefault.dailyFileRetentionDays());
        };

        component.process();
    }

    @Test
    void explicitCompactionMemoryAndEvictionObjectsArePassedByIdentity() throws Exception {
        configureCustom("explicit-context-engineering");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.compaction = CompactionConfig.builder().triggerMessages(7).keepMessages(3).build();
        component.memory = MemoryConfig.builder().sessionRetentionDays(4).build();
        component.eviction = ToolResultEvictionConfig.builder()
                .maxResultChars(1234)
                .previewChars(12)
                .build();
        component.inspectBuilder = builder -> {
            assertSame(component.compaction, field(builder, "compactionConfig"));
            assertSame(component.memory, field(builder, "memoryConfig"));
            assertSame(component.eviction, field(builder, "toolResultEvictionConfig"));
        };

        component.process();
    }

    @Test
    void compactionTriggersThroughThePublicComponentAndPreservesTheTail() throws Exception {
        configureCustom("real-compaction");
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.compaction = CompactionConfig.builder()
                .triggerMessages(3)
                .keepMessages(1)
                .keepTokens(0)
                .flushBeforeCompact(false)
                .offloadBeforeCompact(false)
                .build();

        component.process();
        component.process();

        assertTrue(model.messages.stream().flatMap(Collection::stream)
                .anyMatch(msg -> ConversationCompactor.SUMMARY_MSG_NAME.equals(msg.getName())));
        List<Msg> state = component.runtime.agent().getDelegate()
                .getAgentState(
                        component.lastContext.getRuntimeUserId(),
                        component.lastContext.getRuntimeSessionId())
                .getContext();
        assertTrue(state.stream()
                .anyMatch(msg -> ConversationCompactor.SUMMARY_MSG_NAME.equals(msg.getName())));
        assertTrue(state.stream().anyMatch(msg -> "question".equals(msg.getTextContent())));
    }

    @Test
    void memoryFlushUsesPhysicalRuntimeSessionRootsAcrossAgentsAndConversations()
            throws Exception {
        String namespace = "real-memory";
        configureGuarded(namespace);
        String daily = "memory/" + LocalDate.now() + ".md";
        List<String> markers = List.of("A-ONE", "A-TWO", "B-ONE", "B-TWO");
        List<TestComponent> matrix = List.of(
                memoryComponent(null, "capabilities-agent-a", "conversation-one", markers.get(0)),
                memoryComponent(null, "capabilities-agent-a", "conversation-two", markers.get(1)),
                memoryComponent(null, "capabilities-agent-b", "conversation-one", markers.get(2)),
                memoryComponent(null, "capabilities-agent-b", "conversation-two", markers.get(3)));

        for (TestComponent component : matrix) {
            component.process();
        }

        Path workspace = tempDir.resolve(namespace);
        List<Path> physicalMemoryFiles = matrix.stream()
                .map(component -> workspace
                        .resolve("agent-" + sha256(component.lastContext.getAgentNamespace()))
                        .resolve("session-" + sha256(component.lastContext.getRuntimeSessionId()))
                        .resolve(daily))
                .toList();
        assertEquals(4, Set.copyOf(physicalMemoryFiles).size());
        for (int index = 0; index < physicalMemoryFiles.size(); index++) {
            String content = Files.readString(physicalMemoryFiles.get(index));
            assertTrue(content.contains(markers.get(index)), content);
            for (int other = 0; other < markers.size(); other++) {
                if (other != index) {
                    assertFalse(content.contains(markers.get(other)), content);
                }
            }
        }
        AbstractFilesystem guarded = matrix.get(0).configuredFilesystem;
        List<RuntimeContext> contexts = new ArrayList<>();
        for (int index = 0; index < matrix.size(); index++) {
            TestComponent component = matrix.get(index);
            RuntimeContext context = RuntimeContext.builder()
                    .userId(component.lastContext.getRuntimeUserId())
                    .sessionId(component.lastContext.getRuntimeSessionId())
                    .put(LiteFlowAgentContext.class, component.lastContext)
                    .build();
            contexts.add(context);
            String visible = guarded.read(context, daily, 0, 0).fileData().content();
            assertTrue(visible.contains(markers.get(index)), visible);
            assertEquals(1, markers.stream().filter(visible::contains).count(), visible);
        }
        assertTrue(guarded.write(contexts.get(0), "workspace-shared.txt", "shared").isSuccess());
        assertEquals("shared", guarded.read(contexts.get(2), "workspace-shared.txt", 0, 0)
                .fileData().content());
        assertFalse(guarded.read(contexts.get(1), "workspace-shared.txt", 0, 0).isSuccess());
        assertFalse(Files.exists(workspace.resolve(daily)));
        assertFalse(Files.exists(workspace.resolve("conversation-one").resolve(daily)));
        assertFalse(Files.exists(workspace.resolve("conversation-two").resolve(daily)));
    }

    @Test
    void skillRepositoriesRemainAdditiveAndTheAllowlistReachesHarness() throws Exception {
        configureCustom("skills");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        RecordingRepository first = new RecordingRepository("first");
        RecordingRepository second = new RecordingRepository("second");
        component.repositories = List.of(first, second);
        component.skillFilter = SkillFilter.only("allowed-skill");
        component.inspectBuilder = builder -> {
            assertEquals(List.of(first, second), field(builder, "skillRepositories"));
            assertSame(component.skillFilter, field(builder, "skillFilter"));
        };

        component.process();

        assertEquals(List.of(first, second), component.runtime.agent().getSkillRepositories());
        assertTrue(component.skillFilter.isAllowed("allowed-skill"));
        assertFalse(component.skillFilter.isAllowed("blocked-skill"));
    }

    @Test
    void filteredSkillReachesTheRealModelAndSuccessfulLoadIsTracked() throws Exception {
        configureCustom("real-skills");
        AgentSkill allowed = skill("allowed", "ALLOWED-SKILL-DESCRIPTION");
        AgentSkill blocked = skill("blocked", "BLOCKED-SKILL-DESCRIPTION");
        RecordingRepository repository = new RecordingRepository(
                "real", List.of(allowed, blocked));
        RecordingModel model = RecordingModel.loadSkill(allowed);
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.repositories = List.of(repository);
        // Harness 2.0.2 applies the visibility filter to the skill name; the rendered/tool
        // catalog remains keyed by the source-qualified skill id.
        component.skillFilter = SkillFilter.only(allowed.getName());
        component.dynamicSkills = true;
        component.permission = allow("load_skill_through_path");

        component.process();

        String prompt = model.systemText();
        assertTrue(prompt.contains("ALLOWED-SKILL-DESCRIPTION"), prompt);
        assertFalse(prompt.contains("BLOCKED-SKILL-DESCRIPTION"), prompt);
        List<ToolResultBlock> loadResults = model.messages.stream()
                .flatMap(Collection::stream)
                .flatMap(msg -> msg.getContent().stream())
                .filter(ToolResultBlock.class::isInstance)
                .map(ToolResultBlock.class::cast)
                .filter(block -> "load_skill_through_path".equals(block.getName()))
                .toList();
        assertEquals(1, loadResults.size());
        assertEquals(ToolResultState.SUCCESS, loadResults.get(0).getState());
        assertEquals(List.of(allowed.getSkillId()), component.lastContext.getUsedSkills());
    }

    @Test
    void guardedLocalKeepsSubagentsDisabledWhileRetainingDeclarations() throws Exception {
        configureGuarded("guarded-subagents");
        TestComponent component = component(new RecordingModel(), null);
        SubagentDeclaration declaration = SubagentDeclaration.builder()
                .name("reviewer")
                .description("local reviewer")
                .inlineAgentsBody("Review carefully")
                .inheritParentPermissions(true)
                .build();
        component.subagents = List.of(declaration);
        component.inspectBuilder = builder -> {
            assertEquals(List.of(declaration), field(builder, "subagentDeclarations"));
            assertTrue(booleanField(builder, "disableSubagents"));
        };

        component.process();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("subagentCapableBackends")
    void declaredSubagentInheritsTheEffectiveParentPermissionContext(
            HarnessFilesystemBackend backend) throws Exception {
        configure("subagent-permission-" + backend, backend);
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        if (backend == HarnessFilesystemBackend.DOCKER) {
            component.dockerClient = new FakeSandboxClient(new CopyOnWriteArrayList<>());
        }
        component.permission = ask("execute");
        component.subagents = List.of(SubagentDeclaration.builder()
                .name("permission-child")
                .description("permission child")
                .inlineAgentsBody("child")
                .workspaceMode(WorkspaceMode.SHARED)
                .inheritParentPermissions(true)
                .build());
        component.process();
        RuntimeContext parent = RuntimeContext.builder()
                .userId(component.lastContext.getRuntimeUserId())
                .sessionId(component.lastContext.getRuntimeSessionId())
                .put(LiteFlowAgentContext.class, component.lastContext)
                .build();
        PermissionContextState otherPermissions = allow("execute");
        RuntimeContext otherParent = RuntimeContext.builder(parent)
                .sessionId(parent.getSessionId() + "-other")
                .build();
        component.runtime.agent().getDelegate().replacePermissionContext(
                otherParent.getUserId(), otherParent.getSessionId(), otherPermissions);

        Agent created = component.runtime.agent().getSubagentAgentManager()
                .createAgent("permission-child", parent);
        Agent otherCreated = component.runtime.agent().getSubagentAgentManager()
                .createAgent("permission-child", otherParent);
        try (HarnessAgent child = (HarnessAgent) created;
                HarnessAgent otherChild = (HarnessAgent) otherCreated) {
            PermissionContextState inherited = child.getDelegate()
                    .getAgentState(parent.getUserId(), "child-session")
                    .getPermissionContext();
            PermissionContextState otherInherited = otherChild.getDelegate()
                    .getAgentState(otherParent.getUserId(), "other-child-session")
                    .getPermissionContext();
            assertSame(component.permission, inherited);
            assertSame(otherPermissions, otherInherited);
            assertEquals(PermissionMode.DEFAULT, inherited.getMode());
            assertTrue(inherited.getAskRules().containsKey("execute"), inherited.toString());
            assertTrue(otherInherited.getAllowRules().containsKey("execute"),
                    otherInherited.toString());
        }
    }

    static Stream<HarnessFilesystemBackend> subagentCapableBackends() {
        return Stream.of(HarnessFilesystemBackend.CUSTOM, HarnessFilesystemBackend.DOCKER);
    }

    @ParameterizedTest(name = "{0}, dynamic={1}")
    @MethodSource("subagentSpawnModes")
    void realAgentSpawnInheritsCurrentParentSessionPermissionsFromItsSelectedManager(
            HarnessFilesystemBackend backend, boolean dynamicSubagents) throws Exception {
        configure("real-spawn-" + backend + "-" + dynamicSubagents, backend);
        RecordingModel model = new RecordingModel();
        PermissionProbeMiddleware probe = new PermissionProbeMiddleware("permission-child");
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        if (backend == HarnessFilesystemBackend.DOCKER) {
            component.dockerClient = new FakeSandboxClient(new CopyOnWriteArrayList<>());
        }
        component.middlewares = List.of(probe);
        component.disableDynamicSubagents = !dynamicSubagents;
        component.permission = allow("agent_spawn");
        component.subagents = List.of(SubagentDeclaration.builder()
                .name("permission-child")
                .description("permission child")
                .inlineAgentsBody("child")
                .workspaceMode(WorkspaceMode.SHARED)
                .inheritParentPermissions(true)
                .build());
        component.process();
        PermissionContextState currentSessionPermissions = fullParentPermissions();
        component.runtime.agent().getDelegate().replacePermissionContext(
                component.lastContext.getRuntimeUserId(),
                component.lastContext.getRuntimeSessionId(),
                currentSessionPermissions);

        model.armTool(
                "agent_spawn",
                Map.of(
                        "agent_id", "permission-child",
                        "task", "inspect permissions",
                        "timeout_seconds", 5),
                "{\"agent_id\":\"permission-child\",\"task\":\"inspect permissions\","
                        + "\"timeout_seconds\":5}");
        component.process();

        assertEquals(1, probe.permissions.size(), probe.permissions.toString());
        assertSame(currentSessionPermissions, probe.permissions.get(0));
        assertEquals(PermissionMode.DONT_ASK, probe.permissions.get(0).getMode());
        assertTrue(probe.permissions.get(0).getAllowRules().containsKey("agent_spawn"));
        assertTrue(probe.permissions.get(0).getAskRules().containsKey("sensitive-operation"));
        assertTrue(probe.permissions.get(0).getDenyRules().containsKey("blocked-operation"));
    }

    static Stream<Arguments> subagentSpawnModes() {
        return Stream.of(HarnessFilesystemBackend.CUSTOM, HarnessFilesystemBackend.DOCKER)
                .flatMap(backend -> Stream.of(true, false)
                        .map(dynamic -> Arguments.of(backend, dynamic)));
    }

    @Test
    void concurrentDynamicRefreshNeverPublishesARawInheritingFactory() throws Exception {
        configureCustom("concurrent-dynamic-spawn");
        DynamicRefreshRace race = new DynamicRefreshRace();
        ConcurrentSpawnModel model = new ConcurrentSpawnModel();
        PermissionProbeMiddleware probe = new PermissionProbeMiddleware("permission-child");
        TestComponent component = component(model, new RecordingFilesystem(Map.of(
                "subagents/permission-child.md",
                "---\ndescription: permission child\nworkspace:\n  mode: shared\n---\nchild")));
        component.middlewares = List.of(probe);
        // Install this test-only middleware directly so its order=0 hook observes the exact
        // DynamicSubagentsMiddleware(order=1) -> permission bridge(order=MIN+1) window.
        component.mutateBuilder = builder -> builder.middleware(race);
        component.permission = allow("agent_spawn");
        component.process();
        List<MiddlewareBase> installed = component.runtime.agent().getDelegate().getMiddlewares();
        int dynamicIndex = middlewareIndex(installed, "DynamicSubagentsMiddleware");
        int raceIndex = installed.indexOf(race);
        int bridgeIndex = middlewareIndex(installed, "DynamicRefreshGuard");
        assertTrue(dynamicIndex >= 0 && raceIndex > dynamicIndex && bridgeIndex > raceIndex,
                () -> installed.stream()
                        .map(middleware -> middleware.getClass().getSimpleName()
                                + ":" + middleware.order())
                        .toList().toString());
        race.manager = component.runtime.agent().getSubagentAgentManager();

        PermissionContextState firstPermissions = fullParentPermissions();
        PermissionContextState secondPermissions = ask("other-sensitive-operation");
        HarnessAgent parent = component.runtime.agent();
        parent.getDelegate().replacePermissionContext("race-user", "race-a", firstPermissions);
        parent.getDelegate().replacePermissionContext("race-user", "race-b", secondPermissions);
        RuntimeContext first = RuntimeContext.builder()
                .userId("race-user")
                .sessionId("race-a")
                .put(LiteFlowAgentContext.class, component.lastContext)
                .build();
        RuntimeContext second = RuntimeContext.builder()
                .userId("race-user")
                .sessionId("race-b")
                .put(LiteFlowAgentContext.class, component.lastContext)
                .build();

        CompletableFuture<Msg> firstCall = CompletableFuture.supplyAsync(
                () -> parent.call("spawn-a", first).block());
        if (!race.firstModelEntered.await(5, TimeUnit.SECONDS)) {
            firstCall.get(1, TimeUnit.SECONDS);
            throw new AssertionError("first model barrier was not reached");
        }
        CompletableFuture<Msg> secondCall = CompletableFuture.supplyAsync(
                () -> parent.call("hold-b", second).block());
        PermissionContextState publishedPermissions;
        try {
            assertTrue(race.secondRefreshPublished.await(5, TimeUnit.SECONDS));
            publishedPermissions = inheritedPermissions(
                    race.publishedFactory.create(first), first);
            race.allowFirstModel.countDown();
            firstCall.get(10, TimeUnit.SECONDS);
        }
        finally {
            race.allowFirstModel.countDown();
            race.allowSecondReasoning.countDown();
        }
        secondCall.get(10, TimeUnit.SECONDS);

        String spawnResult = model.toolResultText("agent_spawn");
        assertFalse(spawnResult.contains("status: error"), spawnResult);
        assertEquals(1, probe.permissions.size(), probe.seenAgents + " " + model.describe());
        assertSame(firstPermissions, probe.permissions.get(0));
        assertSame(firstPermissions, publishedPermissions);
    }

    private static int middlewareIndex(List<MiddlewareBase> middlewares, String simpleName) {
        for (int index = 0; index < middlewares.size(); index++) {
            if (simpleName.equals(middlewares.get(index).getClass().getSimpleName())) {
                return index;
            }
        }
        return -1;
    }

    private static PermissionContextState inheritedPermissions(
            Agent child, RuntimeContext parentContext) throws Exception {
        try (HarnessAgent harness = (HarnessAgent) child) {
            return harness.getDelegate()
                    .getAgentState(parentContext.getUserId(), "race-child")
                    .getPermissionContext();
        }
    }

    @ParameterizedTest(name = "dynamic={0}")
    @ValueSource(booleans = {true, false})
    void realAgentSpawnKeepsOptOutIndependentAndRemoteDeclarationRemote(boolean dynamicSubagents)
            throws Exception {
        configureCustom("subagent-permission-opt-out");
        RecordingModel model = new RecordingModel();
        PermissionProbeMiddleware probe = new PermissionProbeMiddleware("independent-child");
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.middlewares = List.of(probe);
        component.disableDynamicSubagents = !dynamicSubagents;
        component.permission = allow("agent_spawn");
        component.subagents = List.of(
                SubagentDeclaration.builder()
                        .name("independent-child")
                        .description("independent")
                        .inlineAgentsBody("child")
                        .workspaceMode(WorkspaceMode.SHARED)
                        .inheritParentPermissions(false)
                        .build(),
                SubagentDeclaration.builder()
                        .name("remote-child")
                        .description("remote")
                        .url("https://invalid.example.test/agent")
                        .inheritParentPermissions(true)
                        .build());
        component.process();
        PermissionContextState currentSessionPermissions = fullParentPermissions();
        component.runtime.agent().getDelegate().replacePermissionContext(
                component.lastContext.getRuntimeUserId(),
                component.lastContext.getRuntimeSessionId(),
                currentSessionPermissions);

        model.armTool(
                "agent_spawn",
                Map.of(
                        "agent_id", "independent-child",
                        "task", "inspect permissions",
                        "timeout_seconds", 5),
                "{\"agent_id\":\"independent-child\",\"task\":\"inspect permissions\","
                        + "\"timeout_seconds\":5}");
        component.process();
        assertEquals(1, probe.permissions.size());
        assertFalse(probe.permissions.get(0).getAskRules().containsKey("sensitive-operation"));
        assertEquals(PermissionMode.DEFAULT, probe.permissions.get(0).getMode());

        model.armTool(
                "agent_spawn",
                Map.of("agent_id", "remote-child"),
                "{\"agent_id\":\"remote-child\"}");
        component.process();
        assertEquals(1, probe.permissions.size(), "remote child must not run the local probe");
        assertTrue(model.toolResultText("agent_spawn").stream()
                .anyMatch(output -> output.contains("agent_id: remote-child")),
                model.toolResultText("agent_spawn").toString());
    }

    @Test
    void taskPlanAndEvictionAreConfiguredWithoutAllowingPlanShell() throws Exception {
        configureCustom("task-plan");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        RecordingTaskRepository tasks = new RecordingTaskRepository();
        component.tasks = tasks;
        component.planMode = true;
        component.eviction = ToolResultEvictionConfig.builder().maxResultChars(2000).build();
        component.inspectBuilder = builder -> {
            assertSame(tasks, field(builder, "taskRepository"));
            assertTrue(booleanField(builder, "planModeEnabled"));
            assertFalse(booleanField(builder, "planModeAllowShell"));
            assertSame(component.eviction, field(builder, "toolResultEvictionConfig"));
        };

        component.process();
    }

    @Test
    void planModePersistsPerSessionAndPermissionNeverDefaultsToBypass() throws Exception {
        configureCustom("real-plan");
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.planMode = true;
        ExecuteTool execute = new ExecuteTool();
        component.tools = List.of(execute);
        component.process();
        HarnessAgent agent = component.runtime.agent();
        RuntimeContext first = RuntimeContext.builder()
                .userId(component.lastContext.getRuntimeUserId())
                .sessionId(component.lastContext.getRuntimeSessionId())
                .build();
        RuntimeContext second = RuntimeContext.builder()
                .userId(component.lastContext.getRuntimeUserId())
                .sessionId(component.lastContext.getRuntimeSessionId() + "-other")
                .build();

        agent.enterPlanMode(first);
        agent.clearStateCache(first);
        model.armTool("execute", Map.of("command", "must-not-run"));
        component.process();

        assertTrue(agent.isPlanModeActive(first));
        assertFalse(agent.isPlanModeActive(second));
        assertEquals(0, execute.executions.get());
        assertTrue(model.messages.stream()
                .flatMap(Collection::stream)
                .flatMap(msg -> msg.getContent().stream())
                .filter(ToolResultBlock.class::isInstance)
                .map(ToolResultBlock.class::cast)
                .anyMatch(result -> "execute".equals(result.getName())
                        && result.getState() == ToolResultState.DENIED));
        assertEquals(PermissionMode.DEFAULT,
                agent.getPermissionMode(first.getUserId(), first.getSessionId()));
        agent.exitPlanMode(first);
        agent.clearStateCache(first);
        assertFalse(agent.isPlanModeActive(first));
    }

    @Test
    void ownedTaskRepositoryIsClosedExactlyOnceWithTheRealHarnessRuntime() throws Exception {
        configureCustom("owned-tasks");
        RecordingTaskRepository tasks = new RecordingTaskRepository();
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.tasks = tasks;
        component.ownTasks = true;

        component.process();
        component.close();
        component.close();

        assertEquals(1, tasks.closeCount.get());
    }

    @Test
    void realTaskListReadsPersistedTasksOnlyFromItsCurrentSession() throws Exception {
        configureCustom("real-task-list");
        RecordingTaskRepository tasks = new RecordingTaskRepository();
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, new RecordingFilesystem(Map.of()));
        component.tasks = tasks;
        component.permission = allow("task_list");
        component.process();
        String user = component.lastContext.getRuntimeUserId();
        String firstSession = component.lastContext.getRuntimeSessionId();
        String secondSession = firstSession + "-other";
        tasks.persist(firstSession, "first-task");
        tasks.persist(secondSession, "second-task");

        model.armTool(
                "task_list",
                Map.of("status_filter", "completed"),
                "{\"status_filter\":\"completed\"}");
        Msg firstReply = component.runtime.agent().call("list first", RuntimeContext.builder()
                .userId(user)
                .sessionId(firstSession)
                .put(LiteFlowAgentContext.class, component.lastContext)
                .build()).block();
        model.armTool(
                "task_list",
                Map.of("status_filter", "completed"),
                "{\"status_filter\":\"completed\"}");
        Msg secondReply = component.runtime.agent().call("list second", RuntimeContext.builder()
                .userId(user)
                .sessionId(secondSession)
                .put(LiteFlowAgentContext.class, component.lastContext)
                .build()).block();

        List<String> outputs = model.toolResultText("task_list");
        assertEquals(2, outputs.size(), "replies="
                + List.of(firstReply.getContent(), secondReply.getContent())
                + ", completedCalls=" + tasks.completedListSessions()
                + ", allCalls=" + tasks.listCalls + ", messages=" + model.messages);
        assertEquals(List.of(firstSession, secondSession),
                tasks.completedListSessions());
        assertTrue(outputs.get(0).contains("first-task"), outputs.toString());
        assertFalse(outputs.get(0).contains("second-task"), outputs.toString());
        assertTrue(outputs.get(1).contains("second-task"), outputs.toString());
        assertFalse(outputs.get(1).contains("first-task"), outputs.toString());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("managedCapabilityAttacks")
    void customizerCannotRemoveOrReplaceManagedCapability(
            String name,
            Consumer<HarnessAgent.Builder> attack,
            String expectedCapability) throws Exception {
        configureCustom("customizer-" + name);
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, new RecordingFilesystem(Map.of(
                "context.md", "managed")));
        component.compaction = CompactionConfig.builder().triggerMessages(5).build();
        component.memory = MemoryConfig.builder().sessionRetentionDays(5).build();
        component.eviction = ToolResultEvictionConfig.builder().maxResultChars(3000).build();
        component.additionalContextFiles = List.of("context.md");
        component.repositories = List.of(new RecordingRepository("managed"));
        component.skillFilter = SkillFilter.only("managed-skill");
        component.subagents = List.of(SubagentDeclaration.builder()
                .name("managed-child")
                .description("managed")
                .inlineAgentsBody("managed")
                .build());
        component.planMode = true;
        component.mutateBuilder = attack;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains(expectedCapability), failure.getMessage());
        assertEquals(0, model.calls.get());
    }

    static Stream<Arguments> managedCapabilityAttacks() {
        return Stream.of(
                Arguments.of("additional-clear",
                        (Consumer<HarnessAgent.Builder>) builder ->
                                mutableList(builder, "additionalContextFiles").clear(),
                        "additional context files"),
                Arguments.of("additional-replace",
                        (Consumer<HarnessAgent.Builder>) builder -> replaceFirst(
                                builder, "additionalContextFiles", "attacker.md"),
                        "additional context files"),
                Arguments.of("skill-repository",
                        (Consumer<HarnessAgent.Builder>) builder -> replaceFirst(
                                builder, "skillRepositories", new RecordingRepository("attacker")),
                        "skill repositories"),
                Arguments.of("skill-filter",
                        (Consumer<HarnessAgent.Builder>) builder -> builder.skillFilter(SkillFilter.none()),
                        "skill filter"),
                Arguments.of("subagents",
                        (Consumer<HarnessAgent.Builder>) builder ->
                                mutableList(builder, "subagentDeclarations").clear(),
                        "subagents"),
                Arguments.of("plan-mode",
                        (Consumer<HarnessAgent.Builder>) builder -> builder.enablePlanMode(false),
                        "plan mode"),
                Arguments.of("compaction",
                        (Consumer<HarnessAgent.Builder>) builder -> builder.compaction(
                                CompactionConfig.builder().triggerMessages(99).build()),
                        "compaction"),
                Arguments.of("memory",
                        (Consumer<HarnessAgent.Builder>) builder -> builder.memory(MemoryConfig.defaults()),
                        "memory"),
                Arguments.of("eviction",
                        (Consumer<HarnessAgent.Builder>) builder -> builder.toolResultEviction(
                                ToolResultEvictionConfig.defaults()),
                        "tool result eviction"));
    }

    @Test
    void customizerMayAppendAdditionalContextSkillsAndSubagentsBeforeRealCall() throws Exception {
        configureCustom("legal-appends");
        RecordingFilesystem filesystem = new RecordingFilesystem(Map.of(
                "managed.md", "MANAGED-CONTEXT",
                "appended.md", "APPENDED-CONTEXT"));
        RecordingRepository managed = new RecordingRepository("managed");
        RecordingRepository appended = new RecordingRepository("appended");
        SubagentDeclaration managedChild = declaration("managed-child");
        SubagentDeclaration appendedChild = declaration("appended-child");
        RecordingModel model = new RecordingModel();
        TestComponent component = component(model, filesystem);
        component.additionalContextFiles = List.of("managed.md");
        component.repositories = List.of(managed);
        component.subagents = List.of(managedChild);
        component.mutateBuilder = builder -> builder
                .additionalContextFile("appended.md")
                .skillRepository(appended)
                .subagent(appendedChild);

        component.process();

        assertTrue(model.systemText().contains("MANAGED-CONTEXT"), model.systemText());
        assertTrue(model.systemText().contains("APPENDED-CONTEXT"), model.systemText());
        assertEquals(List.of(managed, appended), component.runtime.agent().getSkillRepositories());
    }

    private static SubagentDeclaration declaration(String name) {
        return SubagentDeclaration.builder()
                .name(name)
                .description(name)
                .inlineAgentsBody(name)
                .build();
    }

    private static AgentSkill skill(String name, String description) {
        return new AgentSkill(
                Map.of("name", name, "description", description),
                "instructions for " + name,
                Map.of(),
                "memory",
                null);
    }

    private static PermissionContextState allow(String toolName) {
        PermissionRule rule = new PermissionRule(
                toolName, null, PermissionBehavior.ALLOW, "capability test");
        return PermissionContextState.builder().addAllowRule(toolName, rule).build();
    }

    private static PermissionContextState ask(String toolName) {
        PermissionRule rule = new PermissionRule(
                toolName, null, PermissionBehavior.ASK, "capability test");
        return PermissionContextState.builder().addAskRule(toolName, rule).build();
    }

    private static PermissionContextState fullParentPermissions() {
        return PermissionContextState.builder()
                .mode(PermissionMode.DONT_ASK)
                .addAllowRule("agent_spawn", new PermissionRule(
                        "agent_spawn", null, PermissionBehavior.ALLOW, "spawn child"))
                .addAskRule("sensitive-operation", new PermissionRule(
                        "sensitive-operation", null, PermissionBehavior.ASK, "ask parent"))
                .addDenyRule("blocked-operation", new PermissionRule(
                        "blocked-operation", null, PermissionBehavior.DENY, "deny parent"))
                .build();
    }

    private TestComponent component(Model model, AbstractFilesystem filesystem) {
        return component(model, filesystem, "capabilities-agent", "conversation");
    }

    private TestComponent component(
            Model model,
            AbstractFilesystem filesystem,
            String nodeId,
            String conversationId) {
        Slot slot = new Slot();
        slot.setChainId("capabilities-chain");
        slot.setConversationId(conversationId);
        slot.putRequestId("request");
        TestComponent component = new TestComponent(slot, model, filesystem);
        component.setNodeId(nodeId);
        components.add(component);
        return component;
    }

    private TestComponent memoryComponent(
            AbstractFilesystem filesystem,
            String nodeId,
            String conversationId,
            String marker) {
        TestComponent component = component(
                new RecordingModel(), filesystem, nodeId, conversationId);
        component.memory = MemoryConfig.builder()
                .model(new RecordingModel("- " + marker))
                .flushTrigger(MemoryConfig.FlushTrigger.always())
                .build();
        component.memoryHooks = true;
        return component;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException impossible) {
            throw new AssertionError(impossible);
        }
    }

    private void configureCustom(String namespace) throws Exception {
        configure(namespace, HarnessFilesystemBackend.CUSTOM);
    }

    private void configureGuarded(String namespace) throws Exception {
        configure(namespace, HarnessFilesystemBackend.GUARDED_LOCAL);
    }

    private void configure(String namespace, HarnessFilesystemBackend backend) throws Exception {
        Path workspace = tempDir.resolve(namespace);
        Files.createDirectories(workspace);
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("user");
        agent.getWorkspace().setRoot(workspace.toString());
        agent.getHarness().setFilesystemBackend(backend);
        agent.getHarness().setTrustedLocal(backend == HarnessFilesystemBackend.GUARDED_LOCAL);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
    }

    private static Object field(Object target, String name) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            assertTrue(field.trySetAccessible(), name);
            return field.get(target);
        }
        catch (ReflectiveOperationException failure) {
            throw new AssertionError(failure);
        }
    }

    private static boolean booleanField(Object target, String name) {
        return (boolean) field(target, name);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> mutableList(Object target, String name) {
        return (List<Object>) field(target, name);
    }

    private static void replaceFirst(Object target, String name, Object replacement) {
        List<Object> values = mutableList(target, name);
        values.clear();
        values.add(replacement);
    }

    private static final class TestComponent extends HarnessAgentComponent {
        private final Slot slot;
        private final Model model;
        private final AbstractFilesystem filesystem;
        private List<String> additionalContextFiles = List.of();
        private CompactionConfig compaction;
        private MemoryConfig memory;
        private ToolResultEvictionConfig eviction;
        private List<AgentSkillRepository> repositories = List.of();
        private SkillFilter skillFilter = SkillFilter.all();
        private List<SubagentDeclaration> subagents = List.of();
        private TaskRepository tasks;
        private boolean ownTasks;
        private boolean planMode;
        private boolean dynamicSkills;
        private boolean memoryHooks;
        private PermissionContextState permission = PermissionContextState.builder().build();
        private Consumer<HarnessAgent.Builder> inspectBuilder = ignored -> { };
        private Consumer<HarnessAgent.Builder> mutateBuilder = ignored -> { };
        private HarnessAgentRuntime runtime;
        private LiteFlowAgentContext lastContext;
        private SandboxClient<DockerSandboxClientOptions> dockerClient;
        private List<Object> tools = List.of();
        private List<MiddlewareBase> middlewares = List.of();
        private boolean disableDynamicSubagents;
        private AbstractFilesystem configuredFilesystem;

        private TestComponent(Slot slot, Model model, AbstractFilesystem filesystem) {
            this.slot = slot;
            this.model = model;
            this.filesystem = filesystem;
        }

        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("buildModel used"); }

        // PermissionState 断言依赖同一实例引用，测试内用进程内状态存储保持实例同一性。
        @Override protected AgentStateStoreResolver stateStoreResolver() {
            return config -> new ResolvedAgentStateStore(new InMemoryAgentStateStore(), true);
        }
        @Override protected Model buildModel() { return model; }
        protected List<String> additionalContextFiles() { return additionalContextFiles; }
        @Override protected CompactionConfig compactionConfig() { return compaction; }
        @Override protected MemoryConfig memoryConfig() { return memory; }
        @Override protected ToolResultEvictionConfig toolResultEvictionConfig() { return eviction; }
        @Override protected List<AgentSkillRepository> skillRepositories() { return repositories; }
        @Override protected SkillFilter skillFilter() { return skillFilter; }
        @Override protected List<SubagentDeclaration> subagents() { return subagents; }
        @Override protected TaskRepository taskRepository() { return tasks; }
        @Override protected boolean ownsTaskRepository(TaskRepository repository) {
            return ownTasks && repository == tasks;
        }
        @Override protected boolean enablePlanMode() { return planMode; }
        @Override protected PermissionContextState permissionContext() {
            return permission;
        }
        @Override protected List<Object> tools() { return tools; }
        @Override protected List<MiddlewareBase> middlewares() { return middlewares; }
        @Override protected HarnessFilesystemConfigurer filesystemConfigurer() {
            return filesystem == null ? null : (builder, context) -> builder.abstractFilesystem(filesystem);
        }
        @Override protected SandboxClient<DockerSandboxClientOptions> dockerSandboxClient() {
            return dockerClient;
        }
        @Override protected SandboxSnapshotProvider sandboxSnapshotProvider() {
            return ignored -> new NoopSnapshotSpec();
        }
        @Override protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            Object configured = field(builder, "abstractFilesystem");
            if (configured instanceof AbstractFilesystem abstractFilesystem) {
                configuredFilesystem = abstractFilesystem;
            }
            inspectBuilder.accept(builder);
            mutateBuilder.accept(builder);
            if (!dynamicSkills) {
                builder.disableDynamicSkills();
            }
            if (disableDynamicSubagents) {
                builder.disableDynamicSubagents();
            }
            builder.disableDefaultWorkspaceSkills().disableToolsConfig()
                    .disableFilesystemTools().disableShellTool().disableMemoryTools()
                    .disableAtPathExpansion();
            return memoryHooks ? builder : builder.disableMemoryHooks();
        }
        @Override protected void customizeRuntimeContext(
                RuntimeContext.Builder builder, LiteFlowAgentContext context) {
            lastContext = context;
        }
        @Override protected String systemPrompt() { return "Answer deterministically."; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "question"; }
        @Override protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext context) {
            runtime = super.buildRuntime(context);
            return runtime;
        }
    }

    private static final class PermissionProbeMiddleware implements MiddlewareBase {
        private final String childName;
        private final List<PermissionContextState> permissions = new CopyOnWriteArrayList<>();
        private final List<String> seenAgents = new CopyOnWriteArrayList<>();

        private PermissionProbeMiddleware(String childName) {
            this.childName = childName;
        }

        @Override
        public Flux<io.agentscope.core.event.AgentEvent> onReasoning(
                Agent agent,
                RuntimeContext context,
                ReasoningInput input,
                Function<ReasoningInput, Flux<io.agentscope.core.event.AgentEvent>> next) {
            seenAgents.add(agent.getName());
            if (childName.equals(agent.getName())) {
                AgentState state = RuntimeContext.resolveAgentState(context, agent);
                permissions.add(state.getPermissionContext());
            }
            return next.apply(input);
        }
    }

    private static final class DynamicRefreshRace implements MiddlewareBase {
        private final CountDownLatch firstModelEntered = new CountDownLatch(1);
        private final CountDownLatch secondRefreshPublished = new CountDownLatch(1);
        private final CountDownLatch allowFirstModel = new CountDownLatch(1);
        private final CountDownLatch allowSecondReasoning = new CountDownLatch(1);
        private volatile io.agentscope.harness.agent.subagent.DefaultAgentManager manager;
        private volatile io.agentscope.harness.agent.subagent.SubagentFactory publishedFactory;

        @Override public int order() { return 0; }

        @Override
        public Flux<io.agentscope.core.event.AgentEvent> onReasoning(
                Agent agent,
                RuntimeContext context,
                ReasoningInput input,
                Function<ReasoningInput, Flux<io.agentscope.core.event.AgentEvent>> next) {
            if ("race-b".equals(context.getSessionId())) {
                publishedFactory = manager.getAgentFactories().get("permission-child");
                secondRefreshPublished.countDown();
                await(allowSecondReasoning);
            }
            return next.apply(input);
        }

        @Override
        public Flux<io.agentscope.core.event.AgentEvent> onModelCall(
                Agent agent,
                RuntimeContext context,
                ModelCallInput input,
                Function<ModelCallInput, Flux<io.agentscope.core.event.AgentEvent>> next) {
            if ("race-a".equals(context.getSessionId())) {
                firstModelEntered.countDown();
                await(allowFirstModel);
            }
            return next.apply(input);
        }

        private static void await(CountDownLatch latch) {
            try {
                if (!latch.await(10, TimeUnit.SECONDS)) {
                    throw new AssertionError("concurrent refresh barrier timed out");
                }
            }
            catch (InterruptedException failure) {
                Thread.currentThread().interrupt();
                throw new AssertionError(failure);
            }
        }
    }

    private static final class ConcurrentSpawnModel implements Model {
        private final List<List<Msg>> calls = new CopyOnWriteArrayList<>();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            calls.add(List.copyOf(messages));
            String input = messages.stream()
                    .filter(message -> message.getRole() == io.agentscope.core.message.MsgRole.USER)
                    .map(Msg::getTextContent)
                    .reduce((left, right) -> right)
                    .orElse("");
            boolean alreadySpawned = messages.stream()
                    .flatMap(message -> message.getContent().stream())
                    .filter(ToolResultBlock.class::isInstance)
                    .map(ToolResultBlock.class::cast)
                    .anyMatch(result -> "agent_spawn".equals(result.getName()));
            if ("spawn-a".equals(input) && !alreadySpawned) {
                ToolUseBlock use = new ToolUseBlock(
                        "concurrent-spawn",
                        "agent_spawn",
                        Map.of(
                                "agent_id", "permission-child",
                                "task", "inspect permissions",
                                "timeout_seconds", 5),
                        "{\"agent_id\":\"permission-child\","
                                + "\"task\":\"inspect permissions\",\"timeout_seconds\":5}",
                        Map.of(),
                        ToolCallState.PENDING);
                return Flux.just(ChatResponse.builder()
                        .content(List.<ContentBlock>of(use))
                        .finishReason("tool_calls")
                        .build());
            }
            return Flux.just(ChatResponse.builder()
                    .content(List.of(TextBlock.builder().text("done").build()))
                    .finishReason("stop")
                    .build());
        }

        @Override public String getModelName() { return "concurrent-spawn-model"; }

        private String describe() {
            return calls.stream().flatMap(Collection::stream)
                    .flatMap(message -> message.getContent().stream())
                    .map(content -> content instanceof ToolResultBlock result
                            ? "ToolResultBlock:" + result.getName() + ":" + result.getOutput()
                            : content.getClass().getSimpleName() + ":" + content)
                    .toList().toString();
        }

        private String toolResultText(String toolName) {
            return calls.stream().flatMap(Collection::stream)
                    .flatMap(message -> message.getContent().stream())
                    .filter(ToolResultBlock.class::isInstance)
                    .map(ToolResultBlock.class::cast)
                    .filter(result -> toolName.equals(result.getName()))
                    .flatMap(result -> result.getOutput().stream())
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .map(TextBlock::getText)
                    .collect(Collectors.joining("\n"));
        }
    }

    private static final class RecordingModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger toolCalls = new AtomicInteger();
        private final List<List<Msg>> messages = new ArrayList<>();
        private final AgentSkill skillToLoad;
        private final String responseText;
        private String armedTool;
        private Map<String, Object> armedInput = Map.of();
        private String armedRawInput = "{}";
        private RecordingModel() { this(null, "done"); }
        private RecordingModel(String responseText) { this(null, responseText); }
        private RecordingModel(AgentSkill skillToLoad) { this(skillToLoad, "done"); }
        private RecordingModel(AgentSkill skillToLoad, String responseText) {
            this.skillToLoad = skillToLoad;
            this.responseText = responseText;
        }
        private static RecordingModel loadSkill(AgentSkill skill) { return new RecordingModel(skill); }
        private void armTool(String name, Map<String, Object> input) {
            armTool(
                    name,
                    input,
                    input.isEmpty() ? "{}" : "{\"command\":\"must-not-run\"}");
        }
        private void armTool(String name, Map<String, Object> input, String rawInput) {
            armedTool = name;
            armedInput = Map.copyOf(input);
            armedRawInput = rawInput;
        }
        @Override public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            int call = calls.getAndIncrement();
            this.messages.add(List.copyOf(messages));
            if (armedTool != null) {
                String toolName = armedTool;
                assertTrue(tools.stream().anyMatch(tool -> toolName.equals(tool.getName())),
                        () -> "missing tool " + toolName + " in "
                                + tools.stream().map(ToolSchema::getName).toList());
                Map<String, Object> toolInput = armedInput;
                String rawInput = armedRawInput;
                armedTool = null;
                ToolUseBlock use = new ToolUseBlock(
                        "armed-tool-" + toolCalls.incrementAndGet(),
                        toolName,
                        toolInput,
                        rawInput,
                        Map.of(),
                        ToolCallState.PENDING);
                return Flux.just(ChatResponse.builder()
                        .content(List.<ContentBlock>of(use))
                        .finishReason("tool_calls")
                        .build());
            }
            if (call == 0 && skillToLoad != null) {
                assertTrue(tools.stream().anyMatch(tool ->
                        "load_skill_through_path".equals(tool.getName())));
                ToolUseBlock load = new ToolUseBlock(
                        "load-skill",
                        "load_skill_through_path",
                        Map.of("skillId", skillToLoad.getSkillId(), "path", "SKILL.md"),
                        "{\"skillId\":\"" + skillToLoad.getSkillId()
                                + "\",\"path\":\"SKILL.md\"}",
                        Map.of(),
                        ToolCallState.PENDING);
                return Flux.just(ChatResponse.builder()
                        .content(List.<ContentBlock>of(load))
                        .finishReason("tool_calls")
                        .build());
            }
            ContentBlock response = TextBlock.builder().text(responseText).build();
            return Flux.just(ChatResponse.builder().content(List.of(response)).finishReason("stop").build());
        }
        private String systemText() {
            return messages.stream().flatMap(Collection::stream)
                    .flatMap(msg -> msg.getContent().stream())
                    .filter(TextBlock.class::isInstance).map(TextBlock.class::cast)
                    .map(TextBlock::getText).reduce("", (left, right) -> left + "\n" + right);
        }
        private List<String> toolResultText(String toolName) {
            return toolResults().stream()
                    .filter(result -> toolName.equals(result.getName()))
                    .flatMap(result -> result.getOutput().stream())
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .map(TextBlock::getText)
                    .toList();
        }
        private List<ToolResultBlock> toolResults() {
            return messages.stream().flatMap(Collection::stream)
                    .flatMap(msg -> msg.getContent().stream())
                    .filter(ToolResultBlock.class::isInstance)
                    .map(ToolResultBlock.class::cast)
                    .toList();
        }
        @Override public String getModelName() { return "capabilities-model"; }
    }

    private static final class ExecuteTool {
        private final AtomicInteger executions = new AtomicInteger();

        @Tool(name = "execute", concurrencySafe = false)
        public String execute(String command) {
            executions.incrementAndGet();
            return "executed:" + command;
        }
    }

    private static final class RecordingFilesystem implements AbstractFilesystem {
        private final Map<String, String> files;
        private RecordingFilesystem(Map<String, String> files) {
            this.files = new ConcurrentHashMap<>(files);
        }
        @Override public LsResult ls(RuntimeContext context, String path) { throw unused(); }
        @Override public ReadResult read(RuntimeContext context, String path, int offset, int limit) {
            String value = files.get(path);
            return value == null ? ReadResult.fail("not found") : ReadResult.success(FileData.create(value));
        }
        @Override public WriteResult write(RuntimeContext context, String path, String content) {
            files.put(path, content);
            return WriteResult.ok(path);
        }
        @Override public EditResult edit(RuntimeContext context, String path, String oldText, String newText, boolean all) { throw unused(); }
        @Override public GrepResult grep(RuntimeContext context, String pattern, String path, String glob) { throw unused(); }
        @Override public GlobResult glob(RuntimeContext context, String pattern, String path) {
            if ("subagents".equals(path) && "*.md".equals(pattern)) {
                return GlobResult.success(files.keySet().stream()
                        .filter(file -> file.startsWith("subagents/") && file.endsWith(".md"))
                        .map(file -> FileInfo.ofFile(file, files.get(file).length(), ""))
                        .toList());
            }
            return GlobResult.success(List.of());
        }
        @Override public List<FileUploadResponse> uploadFiles(
                RuntimeContext context, List<Map.Entry<String, byte[]>> entries) {
            List<FileUploadResponse> result = new ArrayList<>();
            for (Map.Entry<String, byte[]> entry : entries) {
                String content = new String(entry.getValue(), StandardCharsets.UTF_8);
                files.put(entry.getKey(), content);
                result.add(FileUploadResponse.success(entry.getKey()));
            }
            return result;
        }
        @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext context, List<String> paths) { throw unused(); }
        @Override public WriteResult delete(RuntimeContext context, String path) { throw unused(); }
        @Override public WriteResult move(RuntimeContext context, String from, String to) { throw unused(); }
        @Override public boolean exists(RuntimeContext context, String path) { return files.containsKey(path); }
        private UnsupportedOperationException unused() { return new UnsupportedOperationException(); }
    }

    private static final class RecordingRepository implements AgentSkillRepository {
        private final String name;
        private final List<AgentSkill> skills;
        private RecordingRepository(String name) { this(name, List.of()); }
        private RecordingRepository(String name, List<AgentSkill> skills) {
            this.name = name;
            this.skills = List.copyOf(skills);
        }
        @Override public AgentSkill getSkill(String skillName) {
            return skills.stream().filter(skill -> skill.getName().equals(skillName)
                    || skill.getSkillId().equals(skillName)).findFirst().orElse(null);
        }
        @Override public List<String> getAllSkillNames() {
            return skills.stream().map(AgentSkill::getSkillId).toList();
        }
        @Override public List<AgentSkill> getAllSkills() { return skills; }
        @Override public boolean save(List<AgentSkill> skills, boolean force) { return false; }
        @Override public boolean delete(String skillName) { return false; }
        @Override public boolean skillExists(String skillName) { return getSkill(skillName) != null; }
        @Override public AgentSkillRepositoryInfo getRepositoryInfo() {
            return new AgentSkillRepositoryInfo("memory", name, false);
        }
        @Override public String getSource() { return name; }
        @Override public void setWriteable(boolean writeable) { }
        @Override public boolean isWriteable() { return false; }
    }

    private static final class RecordingTaskRepository implements TaskRepository, AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger();
        private final Map<String, List<BackgroundTask>> persisted = new LinkedHashMap<>();
        private final List<TaskListCall> listCalls = new ArrayList<>();
        private void persist(String session, String taskId) {
            persisted.computeIfAbsent(session, ignored -> new ArrayList<>()).add(
                    new BackgroundTask(taskId, "worker", CompletableFuture.completedFuture("done")));
        }
        @Override public BackgroundTask getTask(RuntimeContext context, String agentId, String taskId) {
            return persisted.getOrDefault(agentId, List.of()).stream()
                    .filter(task -> task.getTaskId().equals(taskId)).findFirst().orElse(null);
        }
        @Override public BackgroundTask putTask(RuntimeContext context, String agentId, String taskId, String description, TaskRunSpec spec) { return null; }
        @Override public void removeTask(RuntimeContext context, String agentId, String taskId) { }
        @Override public void clear() { }
        @Override public Collection<BackgroundTask> listTasks(
                RuntimeContext context, String agentId, TaskStatus status) {
            listCalls.add(new TaskListCall(agentId, status));
            return persisted.getOrDefault(agentId, List.of()).stream()
                    .filter(task -> status == null || task.getTaskStatus() == status)
                    .toList();
        }
        @Override public boolean cancelTask(RuntimeContext context, String agentId, String taskId) { return false; }
        @Override public void close() { closeCount.incrementAndGet(); }
        private List<String> completedListSessions() {
            return listCalls.stream()
                    .filter(call -> call.status() == TaskStatus.COMPLETED)
                    .map(TaskListCall::session)
                    .toList();
        }
        private record TaskListCall(String session, TaskStatus status) { }
    }
}
