package com.yomahub.liteflow.agent.harness.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.harness.sandbox.SandboxSnapshotProvider;
import com.yomahub.liteflow.agent.middleware.AgentMiddlewareOrder;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.Agent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.hook.Hook;
import io.agentscope.core.hook.HookEvent;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.AgentState;
import io.agentscope.core.state.ToolContextState;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.OverlayFilesystem;
import io.agentscope.harness.agent.filesystem.local.LocalFilesystemWithShell;
import io.agentscope.harness.agent.filesystem.model.EditResult;
import io.agentscope.harness.agent.filesystem.model.FileDownloadResponse;
import io.agentscope.harness.agent.filesystem.model.FileUploadResponse;
import io.agentscope.harness.agent.filesystem.model.GlobResult;
import io.agentscope.harness.agent.filesystem.model.GrepResult;
import io.agentscope.harness.agent.filesystem.model.LsResult;
import io.agentscope.harness.agent.filesystem.model.ReadResult;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.filesystem.spec.RemoteFilesystemSpec;
import io.agentscope.harness.agent.filesystem.spec.LocalFilesystemSpec;
import io.agentscope.harness.agent.middleware.DynamicSubagentsMiddleware;
import io.agentscope.harness.agent.middleware.SubagentsMiddleware;
import io.agentscope.harness.agent.middleware.SubagentEntry;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.snapshot.NoopSnapshotSpec;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.DefaultAgentManager;
import io.agentscope.harness.agent.subagent.SubagentSpecGenerator;
import io.agentscope.harness.agent.subagent.task.BackgroundTask;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.TaskRunSpec;
import io.agentscope.harness.agent.subagent.task.TaskStatus;
import io.agentscope.harness.agent.subagent.task.WorkspaceTaskRepository;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import io.agentscope.harness.agent.workspace.WorkspaceIndex;
import io.agentscope.harness.agent.tool.AgentSpawnTool;
import io.agentscope.harness.agent.tool.AgentGenerateTool;
import io.agentscope.harness.agent.tool.ShellExecuteTool;
import io.agentscope.harness.agent.tool.TaskTool;
import io.modelcontextprotocol.spec.McpSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HarnessAgentComponentTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String AGENT_NAMESPACE =
            "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @TempDir
    Path tempDir;

    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void clearConfigAndCloseComponents() {
        components.forEach(TestComponent::close);
        LiteflowConfigGetter.clean();
    }

    @Test
    void componentUsesCoreFinalTemplateAndBuildsOneRuntimeForRepeatedCalls() throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem = new RecordingFilesystem("filesystem", new ArrayList<>());
        RecordingModel model = new RecordingModel("plain reply", false, null, null, null);
        TestComponent component = component(slot("conversation-1", "request-1"), model, filesystem);

        component.process();
        component.process();

        assertEquals("plain reply", component.getSlot().getResponseData());
        assertEquals(1, component.modelBuildCount.get());
        assertEquals(1, component.customizeCount.get());
        assertEquals(2, model.callCount.get());
        assertEquals(
                com.yomahub.liteflow.agent.component.AbstractAgentComponent.class,
                HarnessAgentComponent.class.getMethod("process").getDeclaringClass());
        assertSame(filesystem, component.runtime().agent().getWorkspaceManager().getFilesystem());
    }

    @Test
    void differentSessionsExecuteConcurrentlyOnOneHarnessRuntime() throws Exception {
        configureCustomBackend();
        CountDownLatch entered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        RecordingModel model = new RecordingModel("parallel reply", false, entered, release, null);
        TestComponent component = component(
                slot("unused", "bootstrap"),
                model,
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        Slot firstSlot = slot("conversation-a", "request-a");
        Slot secondSlot = slot("conversation-b", "request-b");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> component.process(firstSlot));
            Future<?> second = executor.submit(() -> component.process(secondSlot));

            assertTrue(entered.await(5, TimeUnit.SECONDS));
            release.countDown();
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
        }
        finally {
            release.countDown();
            executor.shutdownNow();
        }

        assertEquals("parallel reply", firstSlot.getResponseData());
        assertEquals("parallel reply", secondSlot.getResponseData());
        assertEquals(1, component.modelBuildCount.get());
        assertEquals(2, model.callCount.get());
        assertEquals(2, model.runtimeContexts.stream()
                .map(RuntimeContext::getSessionId)
                .distinct()
                .count());
    }

    @Test
    void textJavaTypeAndJsonSchemaOutputsUseTheSharedExecutor() throws Exception {
        configureCustomBackend();

        TestComponent text = component(
                slot("text-session", "text-request"),
                new RecordingModel("text reply", false, null, null, null),
                new RecordingFilesystem("text-filesystem", new ArrayList<>()));
        text.process();
        assertEquals("text reply", text.getSlot().getResponseData());

        TestComponent typed = component(
                slot("typed-session", "typed-request"),
                new RecordingModel("{\"answer\":\"typed reply\"}", true, null, null, null),
                new RecordingFilesystem("typed-filesystem", new ArrayList<>()));
        typed.outputType = StructuredReply.class;
        typed.process();
        StructuredReply typedReply = typed.getSlot().getResponseData();
        assertEquals("typed reply", typedReply.answer);

        TestComponent schema = component(
                slot("schema-session", "schema-request"),
                new RecordingModel("{\"answer\":\"schema reply\"}", true, null, null, null),
                new RecordingFilesystem("schema-filesystem", new ArrayList<>()));
        schema.outputSchema = schema();
        schema.process();
        JsonNode schemaReply = schema.getSlot().getResponseData();
        assertEquals("schema reply", schemaReply.path("answer").asText());
    }

    @Test
    void guardedLocalBuildsOnlyWithTrustAndCustomOrInvalidDockerRemainFailClosed()
            throws Exception {
        AgentConfig config = configureAgent();
        TestComponent untrustedComponent = component(
                slot("backend-session", "backend-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);

        AgentConfigException untrusted =
                assertThrows(AgentConfigException.class, untrustedComponent::process);
        assertTrue(untrusted.getMessage().contains("trusted-local"));
        assertEquals(0, untrustedComponent.modelBuildCount.get());

        config.getHarness().setTrustedLocal(true);
        TestComponent guardedComponent = component(
                slot("guarded-session", "guarded-request"),
                new RecordingModel("guarded reply", false, null, null, null),
                null);
        assertDoesNotThrow(() -> {
            guardedComponent.process();
        });
        assertEquals(1, guardedComponent.modelBuildCount.get());

        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        config.getHarness().getDocker().setCpuCount(0L);
        TestComponent dockerComponent = component(
                slot("docker-session", "docker-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        AgentConfigException docker =
                assertThrows(AgentConfigException.class, dockerComponent::process);
        assertTrue(docker.getMessage().contains("cpu-count"));
        assertEquals(0, dockerComponent.modelBuildCount.get());

        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.CUSTOM);
        TestComponent customComponent = component(
                slot("custom-session", "custom-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        AgentConfigException custom =
                assertThrows(AgentConfigException.class, customComponent::process);
        assertTrue(custom.getMessage().contains("filesystemConfigurer"));
        assertEquals(0, customComponent.modelBuildCount.get());
    }

    @Test
    void dockerBackendUsesTheSnapshotProviderBeforeAnySandboxLifecycleStarts()
            throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        AtomicInteger providerCalls = new AtomicInteger();
        TestComponent component = component(
                slot("docker-provider-session", "docker-provider-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        component.snapshotProvider = context -> {
            providerCalls.incrementAndGet();
            return new NoopSnapshotSpec();
        };
        component.customizeFailure = new AgentConfigException("offline build stop");

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertEquals("offline build stop", failure.getMessage());
        assertEquals(1, providerCalls.get());
    }

    @Test
    void dockerFilesystemStillCannotBeReplacedByTheSameBuilderCustomizer()
            throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        TestComponent component = component(
                slot("docker-replacement-session", "docker-replacement-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        component.customizerDockerFilesystem = new DockerFilesystemSpec();

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("filesystem identity"));
    }

    @Test
    void guardedLocalDisablesWorkspaceDeclaredAndCustomizedSubagentsWithoutBreakingParent()
            throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        Path workspace = Path.of(config.getWorkspace().getRoot());
        Files.createDirectories(workspace.resolve("subagents"));
        Files.writeString(
                workspace.resolve("subagents/preloaded.md"),
                "---\ndescription: preloaded host declaration\n---\npreloaded");
        RecordingModel model =
                new RecordingModel("guarded parent reply", false, null, null, null);
        TestComponent component = component(
                slot("guarded-subagent-session", "guarded-subagent-request"), model, null);
        component.subagentsEnabled = true;
        component.subagentDeclarations = List.of(SubagentDeclaration.builder()
                .name("declared")
                .description("builder declaration")
                .inlineAgentsBody("declared")
                .build());
        component.customizerSubagent = SubagentDeclaration.builder()
                .name("customized")
                .description("customizer declaration")
                .inlineAgentsBody("customized")
                .build();
        AtomicInteger customFactoryCalls = new AtomicInteger();
        component.customizerSubagentFactory = ignored -> {
            customFactoryCalls.incrementAndGet();
            throw new AssertionError("guarded subagent factory must remain unreachable");
        };

        component.process();

        HarnessAgent parent = component.runtime().agent();
        assertEquals("guarded parent reply", component.getSlot().getResponseData());
        assertNull(parent.getSubagentAgentManager());
        assertFalse(parent.getToolkit().getToolSchemas().stream().anyMatch(schema ->
                List.of(
                                "agent_spawn",
                                "agent_send",
                                "agent_list",
                                "task_output",
                                "task_cancel",
                                "task_list")
                        .contains(schema.getName())
                        || ShellExecuteTool.NAME.equals(schema.getName())));
        assertEquals(0, customFactoryCalls.get());

        RuntimeContext callContext = model.runtimeContexts.get(0);
        WorkspaceManager workspaceManager =
                (WorkspaceManager) harnessAgentField("workspaceManager").get(parent);
        assertTrue(workspaceManager.getFilesystem().write(
                callContext,
                "subagents/runtime-added.md",
                "---\ndescription: session declaration\n---\nruntime")
                .isSuccess());

        component.process();

        assertEquals("guarded parent reply", component.getSlot().getResponseData());
        assertNull(parent.getSubagentAgentManager());
        assertEquals(0, customFactoryCalls.get());
    }

    @Test
    void guardedLocalRejectsManualSubagentMiddlewareAndFinalFlagTampering() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        RecordingFilesystem manualFilesystem =
                new RecordingFilesystem("manual-subagent-filesystem", new ArrayList<>());
        WorkspaceManager manualWorkspace =
                new WorkspaceManager(tempDir.resolve("manual-subagent-workspace"), manualFilesystem);
        RecordingTaskRepository manualTasks =
                new RecordingTaskRepository("manual-subagent-tasks", new ArrayList<>());
        SubagentEntry dangerousEntry = new SubagentEntry(
                "manual-danger",
                "manual middleware bypass",
                ignored -> {
                    throw new AssertionError("manual subagent must remain unreachable");
                },
                null);
        TestComponent manual = component(
                slot("manual-guarded-session", "manual-guarded-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        manual.manualSubagentsMiddleware =
                new SubagentsMiddleware(List.of(dangerousEntry), manualTasks, manualWorkspace);

        AgentConfigException manualFailure =
                assertThrows(AgentConfigException.class, manual::process);
        assertTrue(manualFailure.getMessage().contains("SubagentsMiddleware"));

        TestComponent tampered = component(
                slot("tampered-guarded-session", "tampered-guarded-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        tampered.reenableSubagentsReflectively = true;

        AgentConfigException tamperedFailure =
                assertThrows(AgentConfigException.class, tampered::process);
        assertTrue(tamperedFailure.getMessage().contains("subagents to remain disabled"));
    }

    @Test
    void guardedLocalRejectsWrappedOfficialSubagentMiddlewareWithoutTools() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        List<MiddlewareCase> cases = List.of(
                new MiddlewareCase("direct-static", false, 0),
                new MiddlewareCase("direct-dynamic", true, 0),
                new MiddlewareCase("wrapped-static", false, 1),
                new MiddlewareCase("wrapped-dynamic", true, 1),
                new MiddlewareCase("nested-static", false, 2),
                new MiddlewareCase("nested-dynamic", true, 2));
        List<String> unblocked = new ArrayList<>();

        for (MiddlewareCase testCase : cases) {
            Path caseWorkspace = tempDir.resolve(testCase.name());
            Files.createDirectories(caseWorkspace);
            RecordingFilesystem filesystem =
                    new RecordingFilesystem(testCase.name() + "-filesystem", new ArrayList<>());
            WorkspaceManager workspaceManager = new WorkspaceManager(caseWorkspace, filesystem);
            RecordingTaskRepository tasks =
                    new RecordingTaskRepository(testCase.name() + "-tasks", new ArrayList<>());
            SubagentEntry dangerousEntry = new SubagentEntry(
                    "wrapped-danger",
                    "official manager reaches a host-local child",
                    ignored -> unsafeHostLocalChild(caseWorkspace.resolve("child")),
                    null);
            MiddlewareBase official = testCase.dynamic()
                    ? dynamicSubagentsMiddleware(
                            dangerousEntry, filesystem, caseWorkspace, workspaceManager, tasks)
                    : new SubagentsMiddleware(List.of(dangerousEntry), tasks, workspaceManager);
            MiddlewareBase configured = official;
            for (int wrapper = 0; wrapper < testCase.explicitWrapperDepth(); wrapper++) {
                configured = AgentMiddlewareOrder.user(configured);
            }

            RecordingModel model =
                    new RecordingModel("must not run", false, null, null, null);
            TestComponent component = component(
                    slot(testCase.name() + "-session", testCase.name() + "-request"),
                    model,
                    null);
            component.userMiddlewares = List.of(configured);

            try {
                component.process();
            }
            catch (AgentConfigException expected) {
                if (expected.getMessage().contains(official.getClass().getSimpleName())) {
                    continue;
                }
                unblocked.add(testCase.name());
                continue;
            }

            Agent child = officialSubagentManager(official)
                    .createAgent("wrapped-danger", model.runtimeContexts.get(0));
            HarnessAgent dangerousChild = assertInstanceOf(HarnessAgent.class, child);
            try {
                WorkspaceManager childWorkspace = (WorkspaceManager)
                        harnessAgentField("workspaceManager").get(dangerousChild);
                OverlayFilesystem overlay = assertInstanceOf(
                        OverlayFilesystem.class, childWorkspace.getFilesystem());
                assertInstanceOf(LocalFilesystemWithShell.class, overlay.getUpper());
                assertTrue(dangerousChild.getToolkit().getToolSchemas().stream()
                        .anyMatch(schema -> ShellExecuteTool.NAME.equals(schema.getName())));
            }
            finally {
                dangerousChild.close();
            }
            unblocked.add(testCase.name());
        }

        assertEquals(List.of(), unblocked,
                "GUARDED_LOCAL must reject every wrapped official subagent path before build");
    }

    @Test
    void guardedLocalRejectsOfficialSubagentToolsWithoutMiddleware() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        Path workspace = tempDir.resolve("tool-only-subagent");
        Files.createDirectories(workspace);
        RecordingFilesystem filesystem =
                new RecordingFilesystem("tool-only-filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager = new WorkspaceManager(workspace, filesystem);
        RecordingTaskRepository tasks =
                new RecordingTaskRepository("tool-only-tasks", new ArrayList<>());
        SubagentEntry dangerousEntry = new SubagentEntry(
                "tool-only-danger",
                "official tool reaches a host-local child",
                ignored -> unsafeHostLocalChild(workspace.resolve("child")),
                null);
        SubagentsMiddleware official =
                new SubagentsMiddleware(List.of(dangerousEntry), tasks, workspaceManager);
        RecordingModel model = new RecordingModel("must not run", false, null, null, null);
        TestComponent component = component(
                slot("tool-only-session", "tool-only-request"), model, null);
        component.tools = official.getTools();
        assertTrue(component.tools.stream().anyMatch(AgentSpawnTool.class::isInstance));
        assertTrue(component.tools.stream().anyMatch(TaskTool.class::isInstance));

        try {
            component.process();
        }
        catch (AgentConfigException expected) {
            assertTrue(expected.getMessage().contains("subagent tool"));
            return;
        }

        assertTrue(model.toolNames.get(0).containsAll(List.of(
                "agent_spawn",
                "agent_send",
                "agent_list",
                "task_output",
                "task_cancel",
                "task_list")));
        Agent child = official.getAgentManager()
                .createAgent("tool-only-danger", model.runtimeContexts.get(0));
        HarnessAgent dangerousChild = assertInstanceOf(HarnessAgent.class, child);
        try {
            WorkspaceManager childWorkspace =
                    (WorkspaceManager) harnessAgentField("workspaceManager").get(dangerousChild);
            OverlayFilesystem overlay = assertInstanceOf(
                    OverlayFilesystem.class, childWorkspace.getFilesystem());
            assertInstanceOf(LocalFilesystemWithShell.class, overlay.getUpper());
            assertTrue(dangerousChild.getToolkit().getToolSchemas().stream()
                    .anyMatch(schema -> ShellExecuteTool.NAME.equals(schema.getName())));
        }
        finally {
            dangerousChild.close();
        }
        throw new AssertionError(
                "GUARDED_LOCAL must reject official subagent tools before build");
    }

    @Test
    void guardedLocalAuditsOfficialSubagentToolsInInactiveGroupsBeforeStateRestore()
            throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        Path workspace = tempDir.resolve("inactive-subagent-group");
        Files.createDirectories(workspace);
        RecordingFilesystem filesystem =
                new RecordingFilesystem("inactive-group-filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager = new WorkspaceManager(workspace, filesystem);
        RecordingTaskRepository tasks =
                new RecordingTaskRepository("inactive-group-tasks", new ArrayList<>());
        SubagentEntry dangerousEntry = new SubagentEntry(
                "inactive-danger",
                "inactive official group reaches a host-local child",
                ignored -> unsafeHostLocalChild(workspace.resolve("child")),
                null);
        SubagentsMiddleware official =
                new SubagentsMiddleware(List.of(dangerousEntry), tasks, workspaceManager);
        InMemoryAgentStateStore delegate = new InMemoryAgentStateStore();
        RecordingModel model = new RecordingModel("must not run", false, null, null, null);
        TestComponent component = component(
                slot("inactive-session", "inactive-request"), model, null);
        component.inactiveGroupName = "restored-subagents";
        component.groupedTools = official.getTools();
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(delegate, false);
        var identity = new com.yomahub.liteflow.agent.context.InvocationIdentityResolver(
                        config.getRuntime().getNamespace())
                .resolve("test-user", "inactive-session", "harness-agent");
        String runtimeSessionId = identity.runtimeSessionId();
        AgentState restored = AgentState.builder()
                .userId("test-user")
                .sessionId(runtimeSessionId)
                .toolContext(ToolContextState.builder()
                        .addActivatedGroup(component.inactiveGroupName)
                        .build())
                .build();
        delegate.save(
                "test-user",
                identity.storeSessionId(),
                "agent_state",
                restored);

        try {
            component.process();
        }
        catch (AgentConfigException expected) {
            assertTrue(expected.getMessage().contains("subagent tool"));
            return;
        }

        assertTrue(model.toolNames.get(0).containsAll(List.of(
                "agent_spawn",
                "agent_send",
                "agent_list",
                "task_output",
                "task_cancel",
                "task_list")));
        throw new AssertionError(
                "GUARDED_LOCAL must audit inactive registered subagent tools before build");
    }

    @Test
    void guardedLocalRejectsOfficialAgentGenerateTool() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        Path workspace = tempDir.resolve("agent-generate-tool");
        Files.createDirectories(workspace);
        RecordingFilesystem filesystem =
                new RecordingFilesystem("agent-generate-filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager = new WorkspaceManager(workspace, filesystem);
        DefaultAgentManager manager = new DefaultAgentManager(List.of(), workspaceManager);
        RecordingModel model = new RecordingModel("must not run", false, null, null, null);
        TestComponent component = component(
                slot("agent-generate-session", "agent-generate-request"), model, null);
        component.tools = List.of(new AgentGenerateTool(
                new SubagentSpecGenerator(model), manager, filesystem));

        AgentConfigException failure =
                assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("agent_generate"));
        assertEquals(0, model.callCount.get());
    }

    @Test
    void guardedLocalRejectsOfficialSubagentToolsRegisteredOnlyByHookAfterBuild()
            throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        Path workspace = tempDir.resolve("hook-only-subagent");
        Files.createDirectories(workspace);
        RecordingFilesystem filesystem =
                new RecordingFilesystem("hook-only-filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager = new WorkspaceManager(workspace, filesystem);
        RecordingTaskRepository tasks =
                new RecordingTaskRepository("hook-only-tasks", new ArrayList<>());
        SubagentEntry dangerousEntry = new SubagentEntry(
                "hook-only-danger",
                "official hook tool reaches a host-local child",
                ignored -> unsafeHostLocalChild(workspace.resolve("child")),
                null);
        SubagentsMiddleware official =
                new SubagentsMiddleware(List.of(dangerousEntry), tasks, workspaceManager);
        HarnessAgent reachableChild = assertInstanceOf(
                HarnessAgent.class,
                official.getAgentManager().createAgent(
                        "hook-only-danger",
                        RuntimeContext.builder()
                                .userId("test-user")
                                .sessionId("hook-proof-session")
                                .build()));
        try {
            WorkspaceManager childWorkspace = (WorkspaceManager)
                    harnessAgentField("workspaceManager").get(reachableChild);
            OverlayFilesystem overlay = assertInstanceOf(
                    OverlayFilesystem.class, childWorkspace.getFilesystem());
            assertInstanceOf(LocalFilesystemWithShell.class, overlay.getUpper());
            assertTrue(reachableChild.getToolkit().getToolNames().contains(ShellExecuteTool.NAME));
        }
        finally {
            reachableChild.close();
        }
        RecordingModel model = new RecordingModel("must not run", false, null, null, null);
        TestComponent component = component(
                slot("hook-only-session", "hook-only-request"), model, null);
        component.customizerHook = new ToolProvidingHook(official.getTools());

        try {
            component.process();
        }
        catch (AgentConfigException expected) {
            assertTrue(expected.getMessage().contains("subagent tool"));
            assertTrue(component.preparedToolkit.getToolNames().stream()
                    .noneMatch(HarnessAgentComponentTest::isOfficialSubagentTool));
            assertEquals(0, model.callCount.get());
            return;
        }

        HarnessAgent parent = component.runtime().agent();
        assertTrue(component.preparedToolkit.getToolNames().stream()
                .noneMatch(HarnessAgentComponentTest::isOfficialSubagentTool));
        assertTrue(parent.getDelegate().getMiddlewares().stream()
                .noneMatch(middleware -> middleware instanceof SubagentsMiddleware
                        || middleware instanceof DynamicSubagentsMiddleware));
        assertTrue(parent.getToolkit().getToolNames().containsAll(List.of(
                "agent_spawn",
                "agent_send",
                "agent_list",
                "task_output",
                "task_cancel",
                "task_list")));
        throw new AssertionError(
                "GUARDED_LOCAL must validate the final toolkit after hook registration");
    }

    @Test
    void finalGuardedToolkitFailureRollsBackBuiltAgentAndPreparedOwnership() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        List<String> closeOrder = new CopyOnWriteArrayList<>();
        RecordingStore store = new RecordingStore("store", closeOrder, null);
        RecordingModel model = new RecordingModel("must not run", false, null, null, closeOrder);
        model.closeFailure = new RuntimeException("model close failed");
        RecordingFilesystem filesystem = new RecordingFilesystem("filesystem", closeOrder);
        filesystem.closeFailure = new RuntimeException("filesystem close failed");
        RecordingRepository repository =
                new RecordingRepository("repository", closeOrder, null);
        WorkspaceManager workspaceManager =
                new WorkspaceManager(tempDir.resolve("hook-rollback"), filesystem);
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        long schedulersBefore = workspaceTaskSchedulerCount();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                workspaceManager, "hook-rollback-agent", taskExecutor);
        SubagentsMiddleware official = new SubagentsMiddleware(
                List.of(new SubagentEntry(
                        "rollback-danger",
                        "must be rejected after build",
                        ignored -> unsafeHostLocalChild(tempDir.resolve("rollback-child")),
                        null)),
                tasks,
                workspaceManager);
        TestComponent component = component(
                slot("hook-rollback-session", "hook-rollback-request"), model, null);
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(store, true);
        component.repositories = List.of(repository);
        component.ownedRepository = repository;
        component.ownedHarnessResources = List.of(filesystem);
        component.taskRepository = tasks;
        component.ownsTaskRepository = true;
        component.customizerHook = new ToolProvidingHook(official.getTools());
        int stateSaversBefore = shutdownStateSaverCount();

        try {
            AgentConfigException failure =
                    assertThrows(AgentConfigException.class, component::process);

            assertTrue(failure.getMessage().contains("subagent tool"));
            assertEquals(stateSaversBefore, shutdownStateSaverCount());
            assertEquals(List.of("filesystem", "repository", "model", "store"), closeOrder);
            assertEquals(2, failure.getSuppressed().length);
            assertEquals(1, tasks.shutdownCount.get());
            assertEquals(schedulersBefore, workspaceTaskSchedulerCount());
            assertEquals(1, model.closeCount.get());
            assertEquals(1, filesystem.closeCount.get());
        }
        finally {
            if (tasks.shutdownCount.get() == 0) {
                tasks.shutdown();
            }
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void guardedLocalAllowsOrdinaryHookTools() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setTrustedLocal(true);
        RecordingModel model = new RecordingModel("reply", false, null, null, null);
        TestComponent component = component(
                slot("ordinary-hook-session", "ordinary-hook-request"), model, null);
        component.customizerHook = new ToolProvidingHook(List.of(new EchoTool()));

        component.process();

        assertTrue(component.runtime().agent().getToolkit().getToolNames().contains("echo"));
        assertTrue(model.toolNames.get(0).contains("echo"));
    }

    @Test
    void customBackendDoesNotApplyGuardedLocalFinalToolkitPolicy() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("reply", false, null, null, null);
        RecordingFilesystem filesystem =
                new RecordingFilesystem("custom-hook-filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager =
                new WorkspaceManager(tempDir.resolve("custom-hook-workspace"), filesystem);
        SubagentsMiddleware official = new SubagentsMiddleware(
                List.of(new SubagentEntry(
                        "custom-hook-child",
                        "CUSTOM retains its configured subagent policy",
                        ignored -> unsafeHostLocalChild(tempDir.resolve("custom-hook-child")),
                        null)),
                new RecordingTaskRepository("custom-hook-tasks", new ArrayList<>()),
                workspaceManager);
        TestComponent component = component(
                slot("custom-hook-session", "custom-hook-request"), model, filesystem);
        component.customizerHook = new ToolProvidingHook(official.getTools());

        component.process();

        assertTrue(component.runtime().agent().getToolkit().getToolNames()
                        .contains("agent_spawn"),
                component.runtime().agent().getToolkit().getToolNames().toString());
        assertTrue(model.toolNames.get(0).contains("agent_spawn"));
    }

    @Test
    void customConfigurerCannotFallBackToHarnessHostLocalFilesystem() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("must not run", false, null, null, null);
        TestComponent component = component(
                slot("no-op-session", "no-op-request"), model, null);
        component.explicitFilesystemConfigurer = (builder, context) -> { };

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("filesystemConfigurer"));
        assertEquals(1, model.closeCount.get());
    }

    @Test
    void customizerCannotReplaceConfiguredFilesystemOnTheSameBuilder() throws Exception {
        configureCustomBackend();
        RecordingFilesystem configured =
                new RecordingFilesystem("configured-filesystem", new ArrayList<>());
        RecordingFilesystem replacement =
                new RecordingFilesystem("replacement-filesystem", new ArrayList<>());
        TestComponent component = component(
                slot("filesystem-lock-session", "filesystem-lock-request"),
                new RecordingModel("must not run", false, null, null, null),
                configured);
        component.customizerFilesystem = replacement;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("filesystem"));
        assertEquals(0, replacement.closeCount.get());
    }

    @Test
    void customizerCannotReplaceConfiguredWorkspaceOnTheSameBuilder() throws Exception {
        configureCustomBackend();
        TestComponent component = component(
                slot("workspace-lock-session", "workspace-lock-request"),
                new RecordingModel("must not run", false, null, null, null),
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.customizerWorkspace = tempDir.resolve("replacement-workspace");

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("workspace"));
    }

    @Test
    void customizerCannotReplaceEmptyPreparedSerialToolkit() throws Exception {
        configureCustomBackend();
        TestComponent component = component(
                slot("empty-toolkit-session", "empty-toolkit-request"),
                new RecordingModel("must not run", false, null, null, null),
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.replaceToolkit = true;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("Toolkit"));
    }

    @Test
    void customizerCannotReplacePreparedSerialToolkitWithCopyContainingSameTools()
            throws Exception {
        configureCustomBackend();
        TestComponent component = component(
                slot("copied-toolkit-session", "copied-toolkit-request"),
                new RecordingModel("must not run", false, null, null, null),
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.tools = List.of(new EchoTool());
        component.copyToolkit = true;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("Toolkit"));
    }

    @Test
    void borrowedWorkspaceTaskRepositoryFailsBeforeHarnessCanAssumeShutdownOwnership()
            throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem =
                new RecordingFilesystem("filesystem", new ArrayList<>());
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                new WorkspaceManager(tempDir.resolve("borrowed-tasks"), filesystem),
                "borrowed-agent",
                taskExecutor);
        TestComponent component = component(
                slot("borrowed-tasks-session", "borrowed-tasks-request"),
                new RecordingModel("must not run", false, null, null, null),
                filesystem);
        component.taskRepository = tasks;
        component.subagentsEnabled = true;

        try {
            AgentConfigException failure =
                    assertThrows(AgentConfigException.class, component::process);

            assertTrue(failure.getMessage().contains("WorkspaceTaskRepository"));
            assertEquals(0, tasks.shutdownCount.get());
        }
        finally {
            tasks.shutdown();
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void ownedWorkspaceTaskRepositoryIsClosedExactlyOnceByStaticHarnessSubagents()
            throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem =
                new RecordingFilesystem("filesystem", new ArrayList<>());
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                new WorkspaceManager(tempDir.resolve("owned-tasks"), filesystem),
                "owned-agent",
                taskExecutor);
        TestComponent component = component(
                slot("owned-tasks-session", "owned-tasks-request"),
                new RecordingModel("reply", false, null, null, null),
                filesystem);
        component.taskRepository = tasks;
        component.ownsTaskRepository = true;
        component.subagentsEnabled = true;

        try {
            component.process();
            component.close();
            component.close();

            assertEquals(1, tasks.shutdownCount.get());
        }
        finally {
            if (tasks.shutdownCount.get() == 0) {
                tasks.shutdown();
            }
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void ownedWorkspaceTaskRepositoryIsClosedExactlyOnceByDynamicHarnessSubagents()
            throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem =
                new RecordingFilesystem("filesystem", new ArrayList<>());
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                new WorkspaceManager(tempDir.resolve("dynamic-owned-tasks"), filesystem),
                "dynamic-owned-agent",
                taskExecutor);
        TestComponent component = component(
                slot("dynamic-owned-session", "dynamic-owned-request"),
                new RecordingModel("reply", false, null, null, null),
                filesystem);
        component.taskRepository = tasks;
        component.ownsTaskRepository = true;
        component.subagentsEnabled = true;
        component.dynamicSubagentsEnabled = true;

        try {
            component.process();
            component.close();
            component.close();

            assertEquals(1, tasks.shutdownCount.get());
        }
        finally {
            if (tasks.shutdownCount.get() == 0) {
                tasks.shutdown();
            }
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void ownedWorkspaceTaskRepositoryIsClosedByLiteFlowWhenBuiltInsAreDisabled()
            throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem =
                new RecordingFilesystem("filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager =
                new WorkspaceManager(tempDir.resolve("manual-owned-tasks"), filesystem);
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                workspaceManager, "manual-owned-agent", taskExecutor);
        TestComponent component = component(
                slot("manual-owned-session", "manual-owned-request"),
                new RecordingModel("reply", false, null, null, null),
                filesystem);
        component.taskRepository = tasks;
        component.ownsTaskRepository = true;
        component.manualSubagentsMiddleware =
                new SubagentsMiddleware(List.of(), tasks, workspaceManager);

        try {
            component.process();
            component.close();
            component.close();

            assertEquals(1, tasks.shutdownCount.get());
        }
        finally {
            if (tasks.shutdownCount.get() == 0) {
                tasks.shutdown();
            }
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void ownedWorkspaceTaskRepositoryRollbackIsExactlyOnceWithOnlyManualSubagents()
            throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem =
                new RecordingFilesystem("filesystem", new ArrayList<>());
        WorkspaceManager workspaceManager =
                new WorkspaceManager(tempDir.resolve("manual-rollback-tasks"), filesystem);
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                workspaceManager, "manual-rollback-agent", taskExecutor);
        TestComponent component = component(
                slot("manual-rollback-session", "manual-rollback-request"),
                new RecordingModel("reply", false, null, null, null),
                filesystem);
        component.taskRepository = tasks;
        component.ownsTaskRepository = true;
        component.manualSubagentsMiddleware =
                new SubagentsMiddleware(List.of(), tasks, workspaceManager);
        component.replaceStateStore = true;

        try {
            assertThrows(AgentConfigException.class, component::process);

            assertEquals(1, tasks.shutdownCount.get());
        }
        finally {
            if (tasks.shutdownCount.get() == 0) {
                tasks.shutdown();
            }
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void customizerCannotReplaceConfiguredTaskRepositoryIdentity() throws Exception {
        configureCustomBackend();
        RecordingFilesystem filesystem =
                new RecordingFilesystem("filesystem", new ArrayList<>());
        ExecutorService taskExecutor = Executors.newSingleThreadExecutor();
        CountingWorkspaceTaskRepository tasks = new CountingWorkspaceTaskRepository(
                new WorkspaceManager(tempDir.resolve("locked-tasks"), filesystem),
                "locked-agent",
                taskExecutor);
        RecordingTaskRepository replacement =
                new RecordingTaskRepository("replacement", new ArrayList<>());
        TestComponent component = component(
                slot("locked-tasks-session", "locked-tasks-request"),
                new RecordingModel("must not run", false, null, null, null),
                filesystem);
        component.taskRepository = tasks;
        component.ownsTaskRepository = true;
        component.customizerTaskRepository = replacement;

        try {
            AgentConfigException failure =
                    assertThrows(AgentConfigException.class, component::process);

            assertTrue(failure.getMessage().contains("TaskRepository identity"));
            assertEquals(1, tasks.shutdownCount.get());
            assertEquals(0, replacement.closeCount.get());
        }
        finally {
            if (tasks.shutdownCount.get() == 0) {
                tasks.shutdown();
            }
            taskExecutor.shutdownNow();
        }
    }

    @Test
    void missingRemoteStoreFailsBeforeWorkspaceIndexOrTaskSchedulerAllocation()
            throws Exception {
        configureCustomBackend();
        RemoteFilesystemSpec remote = new RemoteFilesystemSpec();
        TestComponent component = component(
                slot("remote-preflight-session", "remote-preflight-request"),
                new RecordingModel("must not run", false, null, null, null),
                null);
        component.explicitFilesystemConfigurer =
                (builder, context) -> builder.filesystem(remote);
        component.subagentsEnabled = true;
        long schedulersBefore = workspaceTaskSchedulerCount();

        try {
            AgentConfigException failure =
                    assertThrows(AgentConfigException.class, component::process);

            assertTrue(failure.getMessage().contains("RemoteFilesystemSpec"));
            assertNull(workspaceIndex(remote));
            assertFalse(Files.exists(tempDir.resolve("workspace/.index/workspace.db")));
            assertEquals(schedulersBefore, workspaceTaskSchedulerCount());
        }
        finally {
            WorkspaceIndex leaked = workspaceIndex(remote);
            if (leaked != null) {
                leaked.close();
            }
        }
    }

    @Test
    void closeIsIdempotentUsesReverseOwnershipOrderAndLeavesBorrowedResourcesOpen()
            throws Exception {
        configureCustomBackend();
        List<String> order = new CopyOnWriteArrayList<>();
        RecordingStore ownedStore = new RecordingStore("store", order, null);
        RecordingModel model = new RecordingModel("reply", false, null, null, order);
        RecordingFilesystem ownedFilesystem = new RecordingFilesystem("filesystem", order);
        RecordingMcpClient ownedMcp = new RecordingMcpClient("mcp", order);
        RecordingMcpClient borrowedMcp = new RecordingMcpClient("borrowed-mcp", order);
        RecordingRepository ownedRepository = new RecordingRepository("repository", order, null);
        RecordingRepository borrowedRepository =
                new RecordingRepository("borrowed-repository", order, null);
        RecordingTaskRepository ownedTasks = new RecordingTaskRepository("tasks", order);
        TestComponent component = component(
                slot("ownership-session", "ownership-request"), model, ownedFilesystem);
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(ownedStore, true);
        component.mcpClients = List.of(ownedMcp, borrowedMcp);
        component.ownedMcp = ownedMcp;
        component.repositories = List.of(ownedRepository, borrowedRepository);
        component.ownedRepository = ownedRepository;
        component.taskRepository = ownedTasks;
        component.ownsTaskRepository = true;
        component.ownedHarnessResources = List.of(ownedFilesystem);

        component.process();
        component.close();
        component.close();

        assertEquals(List.of(
                "tasks", "filesystem", "mcp", "repository", "model", "store"), order);
        assertEquals(0, borrowedMcp.closeCount.get());
        assertEquals(0, borrowedRepository.closeCount.get());

        List<String> borrowedOrder = new CopyOnWriteArrayList<>();
        RecordingFilesystem borrowedFilesystem =
                new RecordingFilesystem("borrowed-filesystem", borrowedOrder);
        RecordingTaskRepository borrowedTasks =
                new RecordingTaskRepository("borrowed-tasks", borrowedOrder);
        RecordingStore borrowedStore = new RecordingStore("borrowed-store", borrowedOrder, null);
        TestComponent borrowed = component(
                slot("borrowed-session", "borrowed-request"),
                new RecordingModel("reply", false, null, null, borrowedOrder),
                borrowedFilesystem);
        borrowed.stateStoreResolver = ignored -> new ResolvedAgentStateStore(borrowedStore, false);
        borrowed.taskRepository = borrowedTasks;
        borrowed.process();
        borrowed.close();

        assertEquals(0, borrowedFilesystem.closeCount.get());
        assertEquals(0, borrowedTasks.closeCount.get());
        assertFalse(borrowedOrder.contains("borrowed-store"));
    }

    @Test
    void buildRollbackKeepsPrimaryFailureAndSuppressesEveryCloseFailure() throws Exception {
        configureCustomBackend();
        List<String> order = new CopyOnWriteArrayList<>();
        RuntimeException storeClose = new RuntimeException("store close failed");
        RuntimeException repositoryClose = new RuntimeException("repository close failed");
        RuntimeException primary = new RuntimeException("customize failed");
        RecordingStore store = new RecordingStore("store", order, storeClose);
        RecordingModel model = new RecordingModel("unused", false, null, null, order);
        model.closeFailure = new RuntimeException("model close failed");
        RecordingRepository repository =
                new RecordingRepository("repository", order, repositoryClose);
        RecordingFilesystem filesystem = new RecordingFilesystem("filesystem", order);
        filesystem.closeFailure = new RuntimeException("filesystem close failed");
        TestComponent component = component(
                slot("rollback-session", "rollback-request"), model, filesystem);
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(store, true);
        component.repositories = List.of(repository);
        component.ownedRepository = repository;
        component.ownedHarnessResources = List.of(filesystem);
        component.customizeFailure = primary;

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertSame(primary, thrown);
        assertEquals(List.of("filesystem", "repository", "model", "store"), order);
        assertEquals(4, thrown.getSuppressed().length);
    }

    @Test
    void customizerCannotReplaceLiteFlowNamespacedStateStore() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("unused", false, null, null, new ArrayList<>());
        TestComponent component = component(
                slot("customizer-session", "customizer-request"),
                model,
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.replaceStateStore = true;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("namespaced StateStore"));
        assertEquals(1, model.closeCount.get());
    }

    @Test
    void customizerCannotReplaceThePreparedHarnessBuilder() throws Exception {
        configureCustomBackend();
        RecordingModel model = new RecordingModel("unused", false, null, null, new ArrayList<>());
        TestComponent component = component(
                slot("replacement-session", "replacement-request"),
                model,
                new RecordingFilesystem("filesystem", new ArrayList<>()));
        component.replaceBuilder = true;

        AgentConfigException failure = assertThrows(AgentConfigException.class, component::process);

        assertTrue(failure.getMessage().contains("provided builder"));
        assertEquals(1, model.closeCount.get());
    }

    private TestComponent component(Slot slot, RecordingModel model, RecordingFilesystem filesystem) {
        TestComponent component = new TestComponent(slot, model, filesystem);
        component.setNodeId("harness-agent");
        components.add(component);
        return component;
    }

    private void configureCustomBackend() throws Exception {
        AgentConfig config = configureAgent();
        config.getHarness().setFilesystemBackend(HarnessFilesystemBackend.CUSTOM);
    }

    private AgentConfig configureAgent() throws Exception {
        Path workspace = tempDir.resolve("workspace");
        Files.createDirectories(workspace);
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace("harness-test");
        agent.getRuntime().setDefaultUserId("test-user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(5));
        agent.getWorkspace().setRoot(workspace.toString());
        LiteflowConfig liteflow = new LiteflowConfig();
        liteflow.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(liteflow);
        return agent;
    }

    private static Slot slot(String conversationId, String requestId) {
        Slot slot = new Slot();
        slot.setChainId("harness-chain");
        slot.setConversationId(conversationId);
        slot.putRequestId(requestId);
        return slot;
    }

    private static JsonNode schema() {
        return MAPPER.createObjectNode()
                .put("type", "object")
                .set("properties", MAPPER.createObjectNode()
                        .set("answer", MAPPER.createObjectNode().put("type", "string")));
    }

    private static WorkspaceIndex workspaceIndex(RemoteFilesystemSpec spec) throws Exception {
        Field field = RemoteFilesystemSpec.class.getDeclaredField("workspaceIndex");
        assertTrue(field.trySetAccessible());
        return (WorkspaceIndex) field.get(spec);
    }

    private static Field harnessAgentField(String name) throws Exception {
        Field field = HarnessAgent.class.getDeclaredField(name);
        assertTrue(field.trySetAccessible());
        return field;
    }

    private static DynamicSubagentsMiddleware dynamicSubagentsMiddleware(
            SubagentEntry entry,
            AbstractFilesystem filesystem,
            Path workspace,
            WorkspaceManager workspaceManager,
            TaskRepository tasks) {
        DefaultAgentManager manager =
                new DefaultAgentManager(List.of(entry), workspaceManager);
        return new DynamicSubagentsMiddleware(
                List.of(entry),
                filesystem,
                workspace,
                ignored -> entry.factory(),
                manager,
                null,
                tasks);
    }

    private static DefaultAgentManager officialSubagentManager(MiddlewareBase middleware) {
        if (middleware instanceof SubagentsMiddleware staticMiddleware) {
            return staticMiddleware.getAgentManager();
        }
        return assertInstanceOf(DynamicSubagentsMiddleware.class, middleware).getAgentManager();
    }

    private static HarnessAgent unsafeHostLocalChild(Path workspace) {
        return HarnessAgent.builder()
                .name("unsafe-host-local-child")
                .model(new RecordingModel("unsafe child reply", false, null, null, null))
                .workspace(workspace)
                .filesystem(new LocalFilesystemSpec())
                .stateStore(new InMemoryAgentStateStore())
                .disableSubagents()
                .build();
    }

    private static boolean isOfficialSubagentTool(String name) {
        return List.of(
                        "agent_spawn",
                        "agent_send",
                        "agent_list",
                        "agent_generate",
                        "task_output",
                        "task_cancel",
                        "task_list")
                .contains(name);
    }

    private static int shutdownStateSaverCount() throws Exception {
        Class<?> managerType = Class.forName(
                "io.agentscope.core.shutdown.GracefulShutdownManager");
        Object manager = managerType.getMethod("getInstance").invoke(null);
        Field stateSavers = managerType.getDeclaredField("stateSavers");
        assertTrue(stateSavers.trySetAccessible());
        return ((Map<?, ?>) stateSavers.get(manager)).size();
    }

    private static long workspaceTaskSchedulerCount() {
        return Thread.getAllStackTraces().keySet().stream()
                .filter(Thread::isAlive)
                .filter(thread -> thread.getName().startsWith("ws-task-maint-"))
                .count();
    }

    public static final class StructuredReply {
        public String answer;
    }

    private record MiddlewareCase(String name, boolean dynamic, int explicitWrapperDepth) {
    }

    private static final class TestComponent extends HarnessAgentComponent {
        private final Slot defaultSlot;
        private final ThreadLocal<Slot> invocationSlot = new ThreadLocal<>();
        private final RecordingModel model;
        private final RecordingFilesystem filesystem;
        private final AtomicInteger modelBuildCount = new AtomicInteger();
        private final AtomicInteger customizeCount = new AtomicInteger();
        private AgentStateStoreResolver stateStoreResolver;
        private List<McpClientWrapper> mcpClients = List.of();
        private McpClientWrapper ownedMcp;
        private List<AgentSkillRepository> repositories = List.of();
        private AgentSkillRepository ownedRepository;
        private TaskRepository taskRepository;
        private boolean ownsTaskRepository;
        private List<? extends AutoCloseable> ownedHarnessResources = List.of();
        private Class<?> outputType;
        private JsonNode outputSchema;
        private RuntimeException customizeFailure;
        private boolean replaceStateStore;
        private boolean replaceBuilder;
        private RecordingFilesystem customizerFilesystem;
        private DockerFilesystemSpec customizerDockerFilesystem;
        private Path customizerWorkspace;
        private List<Object> tools = List.of();
        private List<Object> groupedTools = List.of();
        private String inactiveGroupName;
        private List<MiddlewareBase> userMiddlewares = List.of();
        private boolean replaceToolkit;
        private boolean copyToolkit;
        private Toolkit preparedToolkit;
        private boolean subagentsEnabled;
        private boolean dynamicSubagentsEnabled;
        private List<SubagentDeclaration> subagentDeclarations = List.of();
        private SubagentDeclaration customizerSubagent;
        private Function<String, Agent> customizerSubagentFactory;
        private SubagentsMiddleware manualSubagentsMiddleware;
        private boolean reenableSubagentsReflectively;
        private TaskRepository customizerTaskRepository;
        private Hook customizerHook;
        private HarnessFilesystemConfigurer explicitFilesystemConfigurer;
        private SandboxSnapshotProvider snapshotProvider;
        private HarnessAgentRuntime runtime;

        private TestComponent(
                Slot defaultSlot, RecordingModel model, RecordingFilesystem filesystem) {
            this.defaultSlot = defaultSlot;
            this.model = model;
            this.filesystem = filesystem;
        }

        private void process(Slot slot) {
            invocationSlot.set(slot);
            try {
                try {
                    process();
                }
                catch (Exception failure) {
                    throw new RuntimeException(failure);
                }
            }
            finally {
                invocationSlot.remove();
            }
        }

        private HarnessAgentRuntime runtime() {
            return runtime;
        }

        @Override
        public Slot getSlot() {
            Slot current = invocationSlot.get();
            return current == null ? defaultSlot : current;
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            modelBuildCount.incrementAndGet();
            return model;
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            return stateStoreResolver == null ? super.stateStoreResolver() : stateStoreResolver;
        }

        @Override
        protected List<McpClientWrapper> mcpClients() {
            return mcpClients;
        }

        @Override
        protected boolean ownsMcpClient(McpClientWrapper client) {
            return client == ownedMcp;
        }

        @Override
        protected List<AgentSkillRepository> skillRepositories() {
            return repositories;
        }

        @Override
        protected List<Object> tools() {
            return tools;
        }

        @Override
        protected List<MiddlewareBase> middlewares() {
            return userMiddlewares;
        }

        @Override
        protected List<SubagentDeclaration> subagents() {
            return subagentDeclarations;
        }

        @Override
        protected void customizeToolkit(Toolkit toolkit) {
            preparedToolkit = toolkit;
            if (inactiveGroupName != null) {
                toolkit.createToolGroup(inactiveGroupName, "inactive test group", false);
                for (Object tool : groupedTools) {
                    toolkit.registration()
                            .tool(tool)
                            .group(inactiveGroupName)
                            .apply();
                }
            }
        }

        @Override
        protected boolean ownsSkillRepository(AgentSkillRepository repository) {
            return repository == ownedRepository;
        }

        @Override
        protected TaskRepository taskRepository() {
            return taskRepository;
        }

        @Override
        protected boolean ownsTaskRepository(TaskRepository repository) {
            return ownsTaskRepository && repository == taskRepository;
        }

        @Override
        protected List<? extends AutoCloseable> ownedHarnessResources() {
            return ownedHarnessResources;
        }

        @Override
        protected HarnessFilesystemConfigurer filesystemConfigurer() {
            if (explicitFilesystemConfigurer != null) {
                return explicitFilesystemConfigurer;
            }
            if (filesystem == null) {
                return null;
            }
            return (builder, context) -> builder.abstractFilesystem(filesystem);
        }

        @Override
        protected SandboxSnapshotProvider sandboxSnapshotProvider() {
            return snapshotProvider;
        }

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            customizeCount.incrementAndGet();
            if (customizeFailure != null) {
                throw customizeFailure;
            }
            if (replaceStateStore) {
                builder.stateStore(new InMemoryAgentStateStore());
            }
            if (replaceBuilder) {
                return HarnessAgent.builder();
            }
            if (customizerFilesystem != null) {
                builder.abstractFilesystem(customizerFilesystem);
            }
            if (customizerDockerFilesystem != null) {
                builder.filesystem(customizerDockerFilesystem);
            }
            if (customizerWorkspace != null) {
                builder.workspace(customizerWorkspace);
            }
            if (replaceToolkit) {
                builder.toolkit(new Toolkit());
            }
            if (copyToolkit) {
                builder.toolkit(preparedToolkit.copy());
            }
            builder
                    .disableCompaction()
                    .disableToolResultEviction()
                    .disableMemoryTools()
                    .disableMemoryHooks()
                    .disableWorkspaceContext()
                    .disableAtPathExpansion();
            if (subagentsEnabled) {
                if (!dynamicSubagentsEnabled) {
                    builder.disableDynamicSubagents();
                }
            }
            else {
                builder.disableSubagents();
            }
            if (manualSubagentsMiddleware != null) {
                builder.middleware(manualSubagentsMiddleware);
            }
            if (customizerTaskRepository != null) {
                builder.taskRepository(customizerTaskRepository);
            }
            if (customizerHook != null) {
                builder.hook(customizerHook);
            }
            if (customizerSubagent != null) {
                builder.subagent(customizerSubagent);
            }
            if (customizerSubagentFactory != null) {
                builder.subagentFactory("customized-factory", customizerSubagentFactory);
            }
            if (reenableSubagentsReflectively) {
                try {
                    Field disabled = HarnessAgent.Builder.class.getDeclaredField("disableSubagents");
                    if (!disabled.trySetAccessible()) {
                        throw new AssertionError("disableSubagents must be test-inspectable");
                    }
                    disabled.setBoolean(builder, false);
                }
                catch (ReflectiveOperationException failure) {
                    throw new AssertionError(failure);
                }
            }
            return builder
                    .disableDefaultWorkspaceSkills()
                    .disableDynamicSkills()
                    .disableToolsConfig()
                    .disableFilesystemTools()
                    .disableShellTool();
        }

        @Override
        protected PermissionContextState permissionContext() {
            return PermissionContextState.builder().build();
        }

        @Override
        protected Class<?> structuredOutputType() {
            return outputType;
        }

        @Override
        protected JsonNode structuredOutputSchema() {
            return outputSchema;
        }

        @Override
        protected String systemPrompt() {
            return "Answer deterministically.";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "question";
        }

        @Override
        protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            runtime = super.buildRuntime(buildContext);
            return runtime;
        }
    }

    private static final class EchoTool {

        @Tool
        public String echo(String value) {
            return value;
        }
    }

    private static final class ToolProvidingHook implements Hook {
        private final List<Object> tools;

        private ToolProvidingHook(List<Object> tools) {
            this.tools = List.copyOf(tools);
        }

        @Override
        public <T extends HookEvent> Mono<T> onEvent(T event) {
            return Mono.just(event);
        }

        @Override
        public List<Object> tools() {
            return tools;
        }
    }

    private static final class RecordingModel implements Model, AutoCloseable {
        private final String response;
        private final boolean nativeStructured;
        private final CountDownLatch entered;
        private final CountDownLatch release;
        private final List<String> closeOrder;
        private final AtomicInteger callCount = new AtomicInteger();
        private final AtomicInteger closeCount = new AtomicInteger();
        private final List<RuntimeContext> runtimeContexts = new CopyOnWriteArrayList<>();
        private final List<List<String>> toolNames = new CopyOnWriteArrayList<>();
        private RuntimeException closeFailure;

        private RecordingModel(
                String response,
                boolean nativeStructured,
                CountDownLatch entered,
                CountDownLatch release,
                List<String> closeOrder) {
            this.response = response;
            this.nativeStructured = nativeStructured;
            this.entered = entered;
            this.release = release;
            this.closeOrder = closeOrder;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            callCount.incrementAndGet();
            toolNames.add(tools.stream().map(ToolSchema::getName).toList());
            return Flux.deferContextual(context -> {
                runtimeContexts.add(context.get(AgentBase.RUNTIME_CONTEXT_KEY));
                if (entered != null) {
                    entered.countDown();
                }
                if (release != null) {
                    try {
                        if (!release.await(5, TimeUnit.SECONDS)) {
                            return Flux.error(new IllegalStateException("release timed out"));
                        }
                    }
                    catch (InterruptedException failure) {
                        Thread.currentThread().interrupt();
                        return Flux.error(failure);
                    }
                }
                ContentBlock content = TextBlock.builder().text(response).build();
                return Flux.just(ChatResponse.builder()
                        .content(List.of(content))
                        .finishReason("stop")
                        .build());
            });
        }

        @Override
        public boolean supportsNativeStructuredOutput() {
            return nativeStructured;
        }

        @Override
        public String getModelName() {
            return "harness-test-model";
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            if (closeOrder != null) {
                closeOrder.add("model");
            }
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class RecordingFilesystem implements AbstractFilesystem, AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();
        private RuntimeException closeFailure;

        private RecordingFilesystem(String name, List<String> closeOrder) {
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override public LsResult ls(RuntimeContext context, String path) { throw unused(); }
        @Override public ReadResult read(RuntimeContext context, String path, int offset, int limit) { return ReadResult.fail("not found"); }
        @Override public WriteResult write(RuntimeContext context, String path, String content) { throw unused(); }
        @Override public EditResult edit(RuntimeContext context, String path, String oldText, String newText, boolean all) { throw unused(); }
        @Override public GrepResult grep(RuntimeContext context, String pattern, String path, String glob) { throw unused(); }
        @Override public GlobResult glob(RuntimeContext context, String pattern, String path) { throw unused(); }
        @Override public List<FileUploadResponse> uploadFiles(RuntimeContext context, List<Map.Entry<String, byte[]>> files) { throw unused(); }
        @Override public List<FileDownloadResponse> downloadFiles(RuntimeContext context, List<String> paths) { throw unused(); }
        @Override public WriteResult delete(RuntimeContext context, String path) { throw unused(); }
        @Override public WriteResult move(RuntimeContext context, String from, String to) { throw unused(); }
        @Override public boolean exists(RuntimeContext context, String path) { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }

        private static UnsupportedOperationException unused() {
            return new UnsupportedOperationException("filesystem operation is not used");
        }
    }

    private static final class RecordingStore extends InMemoryAgentStateStore {
        private final String name;
        private final List<String> closeOrder;
        private final RuntimeException closeFailure;

        private RecordingStore(String name, List<String> closeOrder, RuntimeException closeFailure) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.closeFailure = closeFailure;
        }

        @Override
        public void close() {
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class RecordingMcpClient extends McpClientWrapper {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingMcpClient(String name, List<String> closeOrder) {
            super(name);
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override public Mono<Void> initialize() { return Mono.empty(); }
        @Override public Mono<List<McpSchema.Tool>> listTools() { return Mono.just(List.of()); }
        @Override public Mono<McpSchema.CallToolResult> callTool(String name, Map<String, Object> args) { return Mono.error(unused()); }
        @Override public Mono<McpSchema.CallToolResult> callTool(String name, Map<String, Object> args, Map<String, Object> meta) { return Mono.error(unused()); }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
        }
    }

    private static final class RecordingRepository implements AgentSkillRepository {
        private final String name;
        private final List<String> closeOrder;
        private final RuntimeException closeFailure;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingRepository(String name, List<String> closeOrder, RuntimeException closeFailure) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.closeFailure = closeFailure;
        }

        @Override public AgentSkill getSkill(String name) { return null; }
        @Override public List<String> getAllSkillNames() { return List.of(); }
        @Override public List<AgentSkill> getAllSkills() { return List.of(); }
        @Override public boolean save(List<AgentSkill> skills, boolean force) { return false; }
        @Override public boolean delete(String skillName) { return false; }
        @Override public boolean skillExists(String skillName) { return false; }
        @Override public AgentSkillRepositoryInfo getRepositoryInfo() { return new AgentSkillRepositoryInfo("test", name, false); }
        @Override public String getSource() { return name; }
        @Override public void setWriteable(boolean writeable) { }
        @Override public boolean isWriteable() { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class RecordingTaskRepository implements TaskRepository, AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();

        private RecordingTaskRepository(String name, List<String> closeOrder) {
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override public BackgroundTask getTask(RuntimeContext context, String agentId, String taskId) { return null; }
        @Override public BackgroundTask putTask(RuntimeContext context, String agentId, String taskId, String description, TaskRunSpec spec) { return null; }
        @Override public void removeTask(RuntimeContext context, String agentId, String taskId) { }
        @Override public void clear() { }
        @Override public Collection<BackgroundTask> listTasks(RuntimeContext context, String agentId, TaskStatus status) { return List.of(); }
        @Override public boolean cancelTask(RuntimeContext context, String agentId, String taskId) { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
        }
    }

    private static final class CountingWorkspaceTaskRepository
            extends WorkspaceTaskRepository {
        private final AtomicInteger shutdownCount = new AtomicInteger();

        private CountingWorkspaceTaskRepository(
                WorkspaceManager workspaceManager,
                String agentId,
                ExecutorService executor) {
            super(workspaceManager, agentId, executor);
        }

        @Override
        public void shutdown() {
            shutdownCount.incrementAndGet();
            super.shutdown();
        }
    }

    private static UnsupportedOperationException unused() {
        return new UnsupportedOperationException("not used");
    }
}
