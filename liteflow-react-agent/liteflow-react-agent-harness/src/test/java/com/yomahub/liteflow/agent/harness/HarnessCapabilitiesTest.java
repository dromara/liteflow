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
import io.agentscope.core.tool.Tool;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileData;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
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
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
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
    void memoryFlushWritesThroughEachTask7AgentNamespaceWithoutCrossAgentLeakage()
            throws Exception {
        configureCustom("real-memory");
        RecordingFilesystem filesystem = new RecordingFilesystem(Map.of());
        String daily = "memory/" + LocalDate.now() + ".md";
        filesystem.expectUploads(daily, 2);
        RecordingModel memoryModel = new RecordingModel("- DURABLE-MEMORY");
        TestComponent first = component(new RecordingModel(), filesystem);
        first.memory = MemoryConfig.builder()
                .model(memoryModel)
                .flushTrigger(MemoryConfig.FlushTrigger.always())
                .build();
        first.memoryHooks = true;
        TestComponent second = component(new RecordingModel(), filesystem);
        second.setNodeId("capabilities-agent-b");
        second.memory = MemoryConfig.builder()
                .model(memoryModel)
                .flushTrigger(MemoryConfig.FlushTrigger.always())
                .build();
        second.memoryHooks = true;

        first.process();
        second.process();

        assertTrue(filesystem.awaitUploads(daily, 5, TimeUnit.SECONDS));
        List<RecordingFilesystem.Upload> memoryWrites = filesystem.uploads.stream()
                .filter(upload -> daily.equals(upload.path()))
                .toList();
        assertEquals(2, memoryWrites.size());
        assertTrue(memoryWrites.stream().allMatch(upload ->
                upload.content().contains("DURABLE-MEMORY")));
        assertEquals(Set.of(first.lastContext.getAgentKey(), second.lastContext.getAgentKey()),
                memoryWrites.stream().map(RecordingFilesystem.Upload::agentKey)
                        .collect(java.util.stream.Collectors.toSet()));
        assertEquals(Set.of(first.lastContext.getAgentNamespace(), second.lastContext.getAgentNamespace()),
                memoryWrites.stream().map(RecordingFilesystem.Upload::agentNamespace)
                        .collect(java.util.stream.Collectors.toSet()));
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

    @Test
    void declaredSubagentOptOutStaysIndependentAndRemoteDeclarationStaysRemote() throws Exception {
        configureCustom("subagent-permission-opt-out");
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
        component.permission = ask("execute");
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
        RuntimeContext parent = RuntimeContext.builder()
                .userId(component.lastContext.getRuntimeUserId())
                .sessionId(component.lastContext.getRuntimeSessionId())
                .build();

        try (HarnessAgent independent = (HarnessAgent) component.runtime.agent()
                .getSubagentAgentManager().createAgent("independent-child", parent)) {
            assertFalse(independent.getDelegate().getPermissionContext()
                    .getAskRules().containsKey("execute"));
        }
        Agent remote = component.runtime.agent().getSubagentAgentManager()
                .createAgent("remote-child", parent);
        assertFalse(remote instanceof HarnessAgent);
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
        TestComponent component = component(new RecordingModel(), new RecordingFilesystem(Map.of()));
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
        component.model.armTool("execute", Map.of("command", "must-not-run"));
        component.process();

        assertTrue(agent.isPlanModeActive(first));
        assertFalse(agent.isPlanModeActive(second));
        assertEquals(0, execute.executions.get());
        assertTrue(component.model.messages.stream()
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

    private TestComponent component(RecordingModel model, RecordingFilesystem filesystem) {
        Slot slot = new Slot();
        slot.setChainId("capabilities-chain");
        slot.setConversationId("conversation");
        slot.putRequestId("request");
        TestComponent component = new TestComponent(slot, model, filesystem);
        component.setNodeId("capabilities-agent");
        components.add(component);
        return component;
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
        private final RecordingModel model;
        private final RecordingFilesystem filesystem;
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

        private TestComponent(Slot slot, RecordingModel model, RecordingFilesystem filesystem) {
            this.slot = slot;
            this.model = model;
            this.filesystem = filesystem;
        }

        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("buildModel used"); }
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
            inspectBuilder.accept(builder);
            mutateBuilder.accept(builder);
            if (!dynamicSkills) {
                builder.disableDynamicSkills();
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

    private static final class RecordingModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
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
                        "armed-tool",
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
        private final List<Upload> uploads = new CopyOnWriteArrayList<>();
        private final Map<String, CountDownLatch> uploadLatches = new ConcurrentHashMap<>();
        private RecordingFilesystem(Map<String, String> files) {
            this.files = new ConcurrentHashMap<>(files);
        }
        private void expectUploads(String path, int count) {
            uploadLatches.put(path, new CountDownLatch(count));
        }
        private boolean awaitUploads(String path, long timeout, TimeUnit unit)
                throws InterruptedException {
            return uploadLatches.get(path).await(timeout, unit);
        }
        @Override public LsResult ls(RuntimeContext context, String path) { throw unused(); }
        @Override public ReadResult read(RuntimeContext context, String path, int offset, int limit) {
            String value = files.get(path);
            return value == null ? ReadResult.fail("not found") : ReadResult.success(FileData.create(value));
        }
        @Override public WriteResult write(RuntimeContext context, String path, String content) {
            files.put(path, content);
            record(context, path, content);
            return WriteResult.ok(path);
        }
        @Override public EditResult edit(RuntimeContext context, String path, String oldText, String newText, boolean all) { throw unused(); }
        @Override public GrepResult grep(RuntimeContext context, String pattern, String path, String glob) { throw unused(); }
        @Override public GlobResult glob(RuntimeContext context, String pattern, String path) {
            return GlobResult.success(List.of());
        }
        @Override public List<FileUploadResponse> uploadFiles(
                RuntimeContext context, List<Map.Entry<String, byte[]>> entries) {
            List<FileUploadResponse> result = new ArrayList<>();
            for (Map.Entry<String, byte[]> entry : entries) {
                String content = new String(entry.getValue(), StandardCharsets.UTF_8);
                files.put(entry.getKey(), content);
                record(context, entry.getKey(), content);
                result.add(FileUploadResponse.success(entry.getKey()));
            }
            return result;
        }
        @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext context, List<String> paths) { throw unused(); }
        @Override public WriteResult delete(RuntimeContext context, String path) { throw unused(); }
        @Override public WriteResult move(RuntimeContext context, String from, String to) { throw unused(); }
        @Override public boolean exists(RuntimeContext context, String path) { return files.containsKey(path); }
        private void record(RuntimeContext context, String path, String content) {
            LiteFlowAgentContext liteFlow = context.get(LiteFlowAgentContext.class);
            uploads.add(new Upload(
                    path,
                    content,
                    liteFlow == null ? null : liteFlow.getAgentKey(),
                    liteFlow == null ? null : liteFlow.getAgentNamespace()));
            CountDownLatch latch = uploadLatches.get(path);
            if (latch != null) {
                latch.countDown();
            }
        }
        private UnsupportedOperationException unused() { return new UnsupportedOperationException(); }
        private record Upload(String path, String content, String agentKey, String agentNamespace) { }
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
