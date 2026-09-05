package com.yomahub.liteflow.agent.harness.state;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.sandbox.FakeSandboxClient;
import com.yomahub.liteflow.agent.harness.sandbox.InMemorySandboxSnapshot;
import com.yomahub.liteflow.agent.harness.sandbox.SandboxSnapshotProvider;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.guard.AgentInvocationScope;
import com.yomahub.liteflow.agent.guard.LocalAgentInvocationGuard;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.hitl.AgentConfirmationHandler;
import com.yomahub.liteflow.agent.middleware.StateStoreFailureMiddleware;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.property.agent.AgentStateStoreFailurePolicy;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.JsonFileAgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.core.tool.Tool;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.model.WriteResult;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossAgentWorkspaceGuardTest {

    @TempDir
    Path tempDir;

    private final List<ProcessComponent> components = new ArrayList<>();
    private Object originalContextAware;

    private static final String AGENT_A =
            "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static final String AGENT_B =
            "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";
    private static final String SESSION_A =
            "lf-1111111111111111111111111111111111111111111111111111111111111111";
    private static final String SESSION_B =
            "lf-2222222222222222222222222222222222222222222222222222222222222222";
    private static final String SANDBOX_A = "sandbox/session/" + SESSION_A;
    private static final String SANDBOX_B = "sandbox/session/" + SESSION_B;

    @AfterEach
    void cleanRuntimeResources() throws Exception {
        components.forEach(ProcessComponent::close);
        LiteflowConfigGetter.clean();
        if (originalContextAware != null) {
            contextAwareField().set(null, originalContextAware);
            originalContextAware = null;
        }
    }

    @Test
    void harnessDeclaresThatEveryInvocationNeedsTheConversationWorkspaceLease()
            throws Exception {
        assertEquals(
                HarnessAgentComponent.class,
                HarnessAgentComponent.class
                        .getDeclaredMethod("requiresWorkspaceLease")
                        .getDeclaringClass());
    }

    @Test
    void agentKeysAreIsolatedWhileSandboxStateIsSharedByConversation() {
        RecordingStore delegate = new RecordingStore();
        HarnessNamespacedAgentStateStore first = store(delegate, AGENT_A);
        HarnessNamespacedAgentStateStore second = store(delegate, AGENT_B);
        TestState firstAgent = new TestState("agent-a");
        TestState secondAgent = new TestState("agent-b");
        TestState toolkit = new TestState("toolkit-a");
        List<TestState> memory = List.of(new TestState("memory-a"));
        TestState sandbox = new TestState("snapshot-a");

        first.save("user", SESSION_A, "agent_state", firstAgent);
        second.save("user", SESSION_A, "agent_state", secondAgent);
        first.save("user", SESSION_A, "memory_messages", memory);
        first.save("user", "declared@parent#user", "toolkit_activeGroups", toolkit);
        first.save(null, SANDBOX_A, "_sandbox_state", sandbox);

        assertSame(firstAgent,
                first.get("user", SESSION_A, "agent_state", TestState.class).orElseThrow());
        assertSame(secondAgent,
                second.get("user", SESSION_A, "agent_state", TestState.class).orElseThrow());
        assertEquals(memory,
                first.getList("user", SESSION_A, "memory_messages", TestState.class));
        assertSame(toolkit,
                first.get("user", "declared@parent#user", "toolkit_activeGroups",
                        TestState.class).orElseThrow());
        assertSame(sandbox,
                second.get(null, SANDBOX_A, "_sandbox_state", TestState.class).orElseThrow());

        RecordingStore.Call firstAgentWrite = delegate.singleSave("agent-a");
        RecordingStore.Call secondAgentWrite = delegate.singleSave("agent-b");
        RecordingStore.Call sandboxWrite = delegate.singleSave("snapshot-a");
        assertFalse(firstAgentWrite.sessionId().equals(secondAgentWrite.sessionId()));
        assertEquals(null, sandboxWrite.userId());
        assertEquals(sandboxWrite,
                delegate.calls().stream()
                        .filter(call -> call.operation().equals("get")
                                && call.key().equals("_sandbox_state"))
                        .map(call -> call.withOperationAndValue("save-one", "snapshot-a"))
                        .findFirst()
                        .orElseThrow());
    }

    @Test
    void everyCrudOperationUsesOneExplicitRouteAndAdministrativeListingIsAgentOnly() {
        RecordingStore delegate = new RecordingStore();
        HarnessNamespacedAgentStateStore store = store(delegate, AGENT_A);
        TestState agent = new TestState("agent");
        TestState sandbox = new TestState("sandbox");

        store.save("user", SESSION_A, "agent_state", agent);
        store.save("user", SESSION_A, "memory_messages", List.of(agent));
        store.save(null, SANDBOX_A, "_sandbox_state", sandbox);

        assertTrue(store.exists("user", SESSION_A));
        assertTrue(store.exists(null, SANDBOX_A));
        assertEquals(Set.of(SESSION_A), store.listSessionIds("user"));
        assertEquals(Set.of(), store.listSessionIds(null),
                "sandbox resume slots must not be advertised as agent sessions");

        store.delete("user", SESSION_A, "agent_state");
        assertTrue(store.get("user", SESSION_A, "agent_state", TestState.class).isEmpty());
        store.delete(null, SANDBOX_A, "_sandbox_state");
        assertTrue(store.get(null, SANDBOX_A, "_sandbox_state", TestState.class).isEmpty());

        store.save("user", SESSION_B, "agent_state", agent);
        store.save(null, SANDBOX_B, "_sandbox_state", sandbox);
        store.delete("user", SESSION_B);
        store.delete(null, SANDBOX_B);
        assertFalse(store.exists("user", SESSION_B));
        assertFalse(store.exists(null, SANDBOX_B));
    }

    @Test
    void conversationWorkspacePhysicalSessionIsAcceptedByTheJsonFileStore() {
        JsonFileAgentStateStore delegate = new JsonFileAgentStateStore(
                tempDir.resolve("json-state"));
        HarnessNamespacedAgentStateStore store = store(delegate, AGENT_A);

        store.save(null, SANDBOX_A, "_sandbox_state", new TestState("snapshot"));

        assertTrue(store.exists(null, SANDBOX_A));
        assertTrue(delegate.exists(null, "harness-workspace." + SESSION_A));
        assertEquals(Set.of(), store.listSessionIds(null));
    }

    @Test
    void realStoresRoundTripEverySupportedLogicalAgentSession() {
        String declared = "declared@parent#user";
        String unicodeAndLong = "子代理@父#用户-" + "x".repeat(96);
        Set<String> logicalSessions = Set.of(SESSION_A, declared, unicodeAndLong);
        List<AgentStateStore> delegates = List.of(
                new InMemoryAgentStateStore(),
                new JsonFileAgentStateStore(tempDir.resolve("json-dynamic-state")));

        for (AgentStateStore delegate : delegates) {
            HarnessNamespacedAgentStateStore store = store(delegate, AGENT_A);

            for (String logicalSession : logicalSessions) {
                store.save("user", logicalSession, "agent_state", new TestState(logicalSession));
            }

            assertEquals(logicalSessions, store.listSessionIds("user"));
            for (String logicalSession : logicalSessions) {
                assertTrue(store.exists("user", logicalSession));
                assertEquals(logicalSession,
                        store.get("user", logicalSession, "agent_state", TestState.class)
                                .orElseThrow().value());
            }

            store.delete("user", declared, "agent_state");
            assertTrue(store.get("user", declared, "agent_state", TestState.class).isEmpty());
            store.delete("user", unicodeAndLong);
            assertFalse(store.exists("user", unicodeAndLong));
            assertEquals(Set.of(SESSION_A, declared), store.listSessionIds("user"));
        }
    }

    @Test
    void realStoreListingRejectsMalformedReservedPhysicalSessionsWithoutLeakingThem() {
        List<AgentStateStore> delegates = List.of(
                new InMemoryAgentStateStore(),
                new JsonFileAgentStateStore(tempDir.resolve("json-malformed-state")));
        for (AgentStateStore delegate : delegates) {
            HarnessNamespacedAgentStateStore store = store(delegate, AGENT_A);
            delegate.save(
                    "user", AGENT_A + ".h1.A", "agent_state", new TestState("malformed"));

            assertThrows(IllegalStateException.class, () -> store.listSessionIds("user"));
        }
    }

    @Test
    void onlyInventoriedAgentKeysAndTheExactSessionSandboxPairAreAccepted() {
        HarnessNamespacedAgentStateStore store = store(new RecordingStore(), AGENT_A);
        TestState state = new TestState("value");

        for (String agentKey : List.of(
                "agent_state", "memory_messages", "toolkit_activeGroups")) {
            store.save("user", SESSION_A, agentKey, state);
        }
        store.save("user", "sub-deadbeef", "agent_state", state);
        store.save("user", "declared@parent#user", "agent_state", state);

        assertThrows(IllegalArgumentException.class,
                () -> store.save(null, SESSION_A, "_sandbox_state", state));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(null, "sandbox/user/" + SESSION_A, "_sandbox_state", state));
        assertThrows(IllegalArgumentException.class,
                () -> store.save(null, "sandbox/agent/" + AGENT_A, "_sandbox_state", state));
        assertThrows(IllegalArgumentException.class,
                () -> store.save("user", SANDBOX_A, "agent_state", state));
        assertThrows(IllegalArgumentException.class,
                () -> store.save("user", SESSION_A, "future_uninventoried_key", state));
        assertThrows(IllegalArgumentException.class,
                () -> store.exists("user", "sandbox/global"));
    }

    @Test
    void bothRoutesFeedTheExistingFailurePolicyAndTerminalCleanupHooks() {
        RuntimeException agentFailure = new RuntimeException("agent load failed");
        RuntimeException workspaceFailure = new RuntimeException("workspace load failed");
        FailingLoadStore delegate = new FailingLoadStore(agentFailure, workspaceFailure);
        HarnessNamespacedAgentStateStore store = store(delegate, AGENT_A);
        RuntimeContext context = RuntimeContext.builder()
                .userId("user")
                .sessionId(SESSION_A)
                .build();

        assertSame(agentFailure, assertThrows(RuntimeException.class,
                () -> store.get("user", SESSION_A, "agent_state", TestState.class)));
        StateStoreFailureMiddleware failFast = new StateStoreFailureMiddleware(
                store, AgentStateStoreFailurePolicy.FAIL_FAST);
        RuntimeException classified = assertThrows(RuntimeException.class,
                () -> failFast.onSystemPrompt(null, context, "prompt").block());
        assertTrue(classified.getMessage().contains("agent load failed"));

        assertSame(workspaceFailure, assertThrows(RuntimeException.class,
                () -> store.get(null, SANDBOX_A, "_sandbox_state", TestState.class)));
        List<String> warnings = new CopyOnWriteArrayList<>();
        StateStoreFailureMiddleware logAndContinue = new StateStoreFailureMiddleware(
                store, AgentStateStoreFailurePolicy.LOG_AND_CONTINUE, warnings::add);
        assertEquals("prompt",
                logAndContinue.onSystemPrompt(null, context, "prompt").block());
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("workspace load failed"));
        assertTrue(store.takeLoadFailure("user", SESSION_A).isEmpty());

        assertSame(workspaceFailure, assertThrows(RuntimeException.class,
                () -> store.get(null, SANDBOX_A, "_sandbox_state", TestState.class)));
        store.clearLoadFailure("user", SESSION_A);
        assertTrue(store.takeLoadFailure("user", SESSION_A).isEmpty());
    }

    @Test
    void publicHarnessCallsFromDifferentAgentsSerializeOneConversationWorkspace()
            throws Exception {
        RecordingGuard guard = configureHarness("cross-agent-serial");
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch firstRelease = new CountDownLatch(1);
        SerialWorkspaceModel model = new SerialWorkspaceModel(firstEntered, firstRelease);
        ProcessComponent first = component("agent-a", slot("shared-conversation", "request-a"), model);
        ProcessComponent second = component("agent-b", slot("shared-conversation", "request-b"), model);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstCall = executor.submit(first::processUnchecked);
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            Future<?> secondCall = executor.submit(second::processUnchecked);
            assertTrue(guard.secondWorkspaceAttempt.await(5, TimeUnit.SECONDS));
            assertEquals(1, model.calls.get(), "second agent must wait before HarnessAgent.call");

            firstRelease.countDown();
            firstCall.get(5, TimeUnit.SECONDS);
            secondCall.get(5, TimeUnit.SECONDS);
        }
        finally {
            executor.shutdownNow();
        }

        assertEquals(2, model.calls.get());
        assertEquals(1, model.maxActive.get());
        assertEquals(2, model.finalWorkspaceValue.get(),
                "serialized filesystem read-modify-write must not lose state");
        assertEquals(guard.workspaceKeys.get(0), guard.workspaceKeys.get(1));
        assertFalse(guard.stateKeys.get(0).equals(guard.stateKeys.get(1)));
    }

    @Test
    void publicHarnessCallsForDifferentConversationsCanEnterInParallel() throws Exception {
        configureHarness("cross-conversation-parallel");
        CountDownLatch bothEntered = new CountDownLatch(2);
        CountDownLatch release = new CountDownLatch(1);
        ParallelModel model = new ParallelModel(bothEntered, release);
        ProcessComponent first = component("agent-a", slot("conversation-a", "request-a"), model);
        ProcessComponent second = component("agent-b", slot("conversation-b", "request-b"), model);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstCall = executor.submit(first::processUnchecked);
            Future<?> secondCall = executor.submit(second::processUnchecked);
            assertTrue(bothEntered.await(5, TimeUnit.SECONDS));
            assertEquals(2, model.maxActive.get());
            release.countDown();
            firstCall.get(5, TimeUnit.SECONDS);
            secondCall.get(5, TimeUnit.SECONDS);
        }
        finally {
            executor.shutdownNow();
        }
    }

    @Test
    void anotherAgentCannotEnterTheConversationWhileHitlIsPending() throws Exception {
        RecordingGuard guard = configureHarness("cross-agent-hitl");
        Sinks.One<List<ConfirmResult>> confirmation = Sinks.one();
        CountDownLatch handlerEntered = new CountDownLatch(1);
        HitlThenReplyModel model = new HitlThenReplyModel();
        AgentConfirmationHandler handler = (event, context) -> {
            handlerEntered.countDown();
            return confirmation.asMono();
        };
        PermissionContextState ask = PermissionContextState.builder()
                .addAskRule("execute", new PermissionRule(
                        "execute", null, PermissionBehavior.ASK, "test"))
                .build();
        ProcessComponent first = component("agent-a", slot("shared-hitl", "request-a"), model);
        ProcessComponent second = component("agent-b", slot("shared-hitl", "request-b"), model);
        first.confirmationHandler = handler;
        first.permissionContext = ask;
        second.confirmationHandler = handler;
        second.permissionContext = ask;

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstCall = executor.submit(first::processUnchecked);
            assertTrue(handlerEntered.await(5, TimeUnit.SECONDS));
            Future<?> secondCall = executor.submit(second::processUnchecked);
            assertTrue(guard.secondWorkspaceAttempt.await(5, TimeUnit.SECONDS));
            assertEquals(1, model.calls.get(), "agent B must wait through A's HITL continuation");
            assertEquals(0, first.executeTool.executions.get(),
                    "permission ASK must suspend before the real Toolkit tool executes");

            ToolUseBlock pending = model.pending.get();
            assertTrue(pending != null);
            confirmation.tryEmitValue(List.of(new ConfirmResult(true, pending)));
            firstCall.get(5, TimeUnit.SECONDS);
            secondCall.get(5, TimeUnit.SECONDS);
        }
        finally {
            executor.shutdownNow();
            confirmation.tryEmitEmpty();
        }

        assertEquals(List.of("agent-a", "agent-a", "agent-b"), model.agentKeys);
        assertEquals(1, first.executeTool.executions.get());
    }

    @Test
    void task6SnapshotIsLoadedDeserializedAndResumedAcrossDifferentAgents() throws Exception {
        configureDockerHarness("snapshot-components");
        PermissionContextState allowExecute = PermissionContextState.builder()
                .addAllowRule("execute", new PermissionRule(
                        "execute", null, PermissionBehavior.ALLOW, "snapshot-test"))
                .build();
        List<String> events = new CopyOnWriteArrayList<>();
        RecordingStore delegate = new RecordingStore();
        InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
        FakeSandboxClient firstClient = new FakeSandboxClient(events);
        ProcessComponent writer = dockerComponent(
                "agent-a",
                slot("snapshot-conversation", "write-request"),
                new ToolThenReplyModel("write durable.txt cross-agent"),
                delegate,
                snapshots,
                firstClient);
        writer.permissionContext = allowExecute;
        writer.processUnchecked();
        writer.close();

        FakeSandboxClient.StateSnapshotIdentity persisted = firstClient.createdStates().get(0);
        assertEquals(1, firstClient.createdStates().size());
        assertEquals(1, snapshots.snapshotCount());

        FakeSandboxClient secondClient = new FakeSandboxClient(events);
        ReadThenReplyModel reader = new ReadThenReplyModel("read durable.txt");
        ProcessComponent rebuilt = dockerComponent(
                "agent-b",
                slot("snapshot-conversation", "read-request"),
                reader,
                delegate,
                snapshots,
                secondClient);
        rebuilt.permissionContext = allowExecute;
        rebuilt.processUnchecked();
        rebuilt.close();

        assertTrue(reader.toolOutput.get().contains("cross-agent"), events.toString());
        assertEquals(0, secondClient.createdStates().size());
        assertEquals(List.of(persisted), secondClient.deserializedWithSnapshotStates());
        assertEquals(List.of(persisted), secondClient.resumedStates());
        assertEquals(1, events.stream()
                .filter(("snapshot-restore:" + persisted.stateId())::equals)
                .count());
        Set<String> agentNamespaces = delegate.calls().stream()
                .filter(call -> "agent_state".equals(call.key()))
                .map(RecordingStore.Call::sessionId)
                .map(session -> session.substring(0, session.indexOf('.')))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> workspaceSessions = delegate.calls().stream()
                .filter(call -> "_sandbox_state".equals(call.key()))
                .map(RecordingStore.Call::sessionId)
                .collect(java.util.stream.Collectors.toSet());
        assertEquals(2, agentNamespaces.size(),
                "different component agentKey namespaces must not cross");
        Set<String> physicalAgentSessions = delegate.calls().stream()
                .filter(call -> call.key() != null && List.of(
                        "agent_state", "memory_messages", "toolkit_activeGroups")
                        .contains(call.key()))
                .map(RecordingStore.Call::sessionId)
                .collect(java.util.stream.Collectors.toSet());
        assertTrue(agentNamespaces.stream().allMatch(namespace -> physicalAgentSessions.stream()
                .anyMatch(session -> session.startsWith(namespace + "."))));
        assertTrue(physicalAgentSessions.stream().allMatch(session -> agentNamespaces.stream()
                .filter(namespace -> session.startsWith(namespace + "."))
                .count() == 1));
        assertEquals(1, workspaceSessions.size(), "conversation snapshot route must be shared");
    }

    @Test
    void preDelegateCreateFailureCannotLeaveSwallowedWorkspaceLoadFailureStale()
            throws Exception {
        LifecycleRecordingGuard guard = configureGuardedDockerHarness("failure-cleanup");
        RuntimeException loadFailure = new RuntimeException("sandbox load failed");
        RuntimeException createFailure = new RuntimeException("sandbox create failed");
        FailFirstSandboxLoadStore delegate = new FailFirstSandboxLoadStore(loadFailure);
        List<String> sandboxEvents = new CopyOnWriteArrayList<>();
        FakeSandboxClient client = new FakeSandboxClient(sandboxEvents, createFailure);
        CountingReplyModel model = new CountingReplyModel();
        Slot slot = slot("failure-conversation", "failure-request");
        ProcessComponent component = dockerComponent(
                "agent-a",
                slot,
                model,
                delegate,
                new InMemorySandboxSnapshot(sandboxEvents),
                client);

        RuntimeException first = assertThrows(RuntimeException.class, component::processUnchecked);

        assertTrue(hasCause(first, createFailure));
        assertEquals(1, client.createAttempts());
        assertEquals(0, model.calls.get());
        assertFalse(slot.hasAttachment(component.lastContext.get().getAttachmentKey()));

        component.processUnchecked();

        assertEquals(2, client.createAttempts());
        assertEquals(1, model.calls.get());
        assertEquals(2, delegate.sandboxLoadAttempts.get());
        assertFalse(slot.hasAttachment(component.lastContext.get().getAttachmentKey()));
        assertEquals(List.of(
                "acquire:WORKSPACE", "acquire:STATE", "close:STATE", "close:WORKSPACE",
                "acquire:WORKSPACE", "acquire:STATE", "close:STATE", "close:WORKSPACE"),
                guard.events);
    }

    @Test
    void publicProcessAcquiresWorkspaceBeforeStateAndRollsWorkspaceBackOnStateFailure()
            throws Exception {
        configureHarness("lease-rollback");
        RuntimeException stateFailure = new RuntimeException("state acquire failed");
        FailingStateGuard guard = new FailingStateGuard(stateFailure);
        contextAwareField().set(null, new GuardContextAware(guard));
        CountingReplyModel model = new CountingReplyModel();
        ProcessComponent component = component(
                "agent-a", slot("lease-conversation", "lease-request"), model);

        RuntimeException thrown = assertThrows(RuntimeException.class, component::processUnchecked);

        assertSame(stateFailure, thrown.getCause());
        assertEquals(List.of(
                "acquire:WORKSPACE", "acquire:STATE", "close:WORKSPACE"), guard.events);
        assertEquals(0, model.calls.get());
    }

    private RecordingGuard configureHarness(String namespace) throws Exception {
        Files.createDirectories(tempDir.resolve("workspace"));
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(5));
        agent.getWorkspace().setRoot(tempDir.resolve("workspace").toString());
        agent.getHarness().setFilesystemBackend(HarnessFilesystemBackend.GUARDED_LOCAL);
        agent.getHarness().setTrustedLocal(true);
        agent.getInvocationGuard().setMode(AgentInvocationGuardMode.BEAN);
        agent.getInvocationGuard().setBeanName("recording-guard");
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);

        RecordingGuard guard = new RecordingGuard();
        Field contextAware = contextAwareField();
        originalContextAware = contextAware.get(null);
        contextAware.set(null, new GuardContextAware(guard));
        return guard;
    }

    private void configureDockerHarness(String namespace) throws Exception {
        Files.createDirectories(tempDir.resolve("docker-workspace"));
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(5));
        agent.getWorkspace().setRoot(tempDir.resolve("docker-workspace").toString());
        agent.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
    }

    private LifecycleRecordingGuard configureGuardedDockerHarness(String namespace)
            throws Exception {
        Files.createDirectories(tempDir.resolve("docker-workspace"));
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(5));
        agent.getWorkspace().setRoot(tempDir.resolve("docker-workspace").toString());
        agent.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        agent.getStateStore().setFailurePolicy(AgentStateStoreFailurePolicy.FAIL_FAST);
        agent.getInvocationGuard().setMode(AgentInvocationGuardMode.BEAN);
        agent.getInvocationGuard().setBeanName("recording-guard");
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);

        LifecycleRecordingGuard guard = new LifecycleRecordingGuard();
        Field contextAware = contextAwareField();
        originalContextAware = contextAware.get(null);
        contextAware.set(null, new GuardContextAware(guard));
        return guard;
    }

    private ProcessComponent component(String nodeId, Slot slot, Model model) {
        ProcessComponent component = new ProcessComponent(slot, model);
        component.setNodeId(nodeId);
        components.add(component);
        return component;
    }

    private ProcessComponent dockerComponent(
            String nodeId,
            Slot slot,
            Model model,
            AgentStateStore delegate,
            InMemorySandboxSnapshot snapshots,
            FakeSandboxClient client) {
        ProcessComponent component = component(nodeId, slot, model);
        component.stateStoreResolver = ignored -> new ResolvedAgentStateStore(delegate, false);
        component.snapshots = ignored -> snapshots;
        component.sandboxClient = client;
        component.docker = true;
        return component;
    }

    private static Slot slot(String conversationId, String requestId) {
        Slot slot = new Slot();
        slot.setChainId("harness-chain");
        slot.setConversationId(conversationId);
        slot.putRequestId(requestId);
        return slot;
    }

    private static Field contextAwareField() throws Exception {
        Field field = ContextAwareHolder.class.getDeclaredField("contextAware");
        field.setAccessible(true);
        return field;
    }

    private static ChatResponse response(String text) {
        ContentBlock block = TextBlock.builder().text(text).build();
        return ChatResponse.builder().content(List.of(block)).finishReason("stop").build();
    }

    private static boolean hasCause(Throwable thrown, Throwable expected) {
        for (Throwable current = thrown; current != null; current = current.getCause()) {
            if (current == expected) {
                return true;
            }
        }
        return false;
    }

    private static HarnessNamespacedAgentStateStore store(
            AgentStateStore delegate, String agentNamespace) {
        return new HarnessNamespacedAgentStateStore(delegate, agentNamespace);
    }

    private static final class ProcessComponent extends HarnessAgentComponent {
        private final Slot slot;
        private final Model model;
        private final ExecuteTool executeTool = new ExecuteTool();
        private AgentConfirmationHandler confirmationHandler;
        private PermissionContextState permissionContext;
        private AgentStateStoreResolver stateStoreResolver;
        private SandboxSnapshotProvider snapshots;
        private SandboxClient<DockerSandboxClientOptions> sandboxClient;
        private final AtomicReference<LiteFlowAgentContext> lastContext = new AtomicReference<>();
        private boolean docker;

        private ProcessComponent(Slot slot, Model model) {
            this.slot = slot;
            this.model = model;
        }

        private void processUnchecked() {
            try {
                process();
            }
            catch (Exception failure) {
                throw new RuntimeException(failure);
            }
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            return model;
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            // 断言依赖状态实例同一性，未显式指定时用进程内状态存储。
            return stateStoreResolver != null ? stateStoreResolver
                    : config -> new ResolvedAgentStateStore(new InMemoryAgentStateStore(), true);
        }

        @Override
        protected SandboxSnapshotProvider sandboxSnapshotProvider() {
            return snapshots;
        }

        @Override
        protected SandboxClient<DockerSandboxClientOptions> dockerSandboxClient() {
            return sandboxClient == null ? super.dockerSandboxClient() : sandboxClient;
        }

        @Override
        protected PermissionContextState permissionContext() {
            return permissionContext == null
                    ? PermissionContextState.builder().build()
                    : permissionContext;
        }

        @Override
        protected void customizeRuntimeContext(
                RuntimeContext.Builder builder, LiteFlowAgentContext context) {
            lastContext.set(context);
        }

        @Override
        protected AgentConfirmationHandler confirmationHandler() {
            return confirmationHandler;
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
        protected List<Object> tools() {
            return docker ? List.of() : List.of(executeTool);
        }

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            HarnessAgent.Builder customized = builder
                    .disableSubagents()
                    .disableCompaction()
                    .disableToolResultEviction()
                    .disableMemoryTools()
                    .disableMemoryHooks()
                    .disableWorkspaceContext()
                    .disableAtPathExpansion()
                    .disableDefaultWorkspaceSkills()
                    .disableDynamicSkills()
                    .disableToolsConfig();
            return docker
                    ? customized
                    : customized.disableFilesystemTools().disableShellTool();
        }
    }

    private static final class ExecuteTool {
        private final AtomicInteger executions = new AtomicInteger();

        @Tool
        public String execute(String command) {
            executions.incrementAndGet();
            return "executed:" + command;
        }
    }

    private static final class SerialWorkspaceModel implements Model {
        private final CountDownLatch firstEntered;
        private final CountDownLatch firstRelease;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();
        private final AtomicInteger finalWorkspaceValue = new AtomicInteger();

        private SerialWorkspaceModel(CountDownLatch firstEntered, CountDownLatch firstRelease) {
            this.firstEntered = firstEntered;
            this.firstRelease = firstRelease;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.deferContextual(context -> {
                RuntimeContext runtime = context.get(
                        io.agentscope.core.agent.AgentBase.RUNTIME_CONTEXT_KEY);
                AbstractFilesystem filesystem = runtime.get(WorkspaceManager.class).getFilesystem();
                int invocation = calls.incrementAndGet();
                int now = active.incrementAndGet();
                maxActive.accumulateAndGet(now, Math::max);
                int before = filesystem.exists(runtime, "counter.txt")
                        ? Integer.parseInt(filesystem.read(runtime, "counter.txt", 0, 32)
                                .fileData().content().trim())
                        : 0;
                try {
                    if (invocation == 1) {
                        firstEntered.countDown();
                        if (!firstRelease.await(5, TimeUnit.SECONDS)) {
                            return Flux.error(new AssertionError("first release timed out"));
                        }
                    }
                    int after = before + 1;
                    if (before == 0) {
                        WriteResult write = filesystem.write(
                                runtime, "counter.txt", Integer.toString(after));
                        assertTrue(write.isSuccess(), write.toString());
                    }
                    else {
                        assertTrue(filesystem.edit(
                                runtime,
                                "counter.txt",
                                Integer.toString(before),
                                Integer.toString(after),
                                false).isSuccess());
                    }
                    finalWorkspaceValue.set(Integer.parseInt(
                            filesystem.read(runtime, "counter.txt", 0, 32)
                                    .fileData().content().trim()));
                    return Flux.just(response("done"));
                }
                catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    return Flux.error(failure);
                }
                finally {
                    active.decrementAndGet();
                }
            });
        }

        @Override
        public String getModelName() {
            return "serial-workspace";
        }
    }

    private static final class ParallelModel implements Model {
        private final CountDownLatch bothEntered;
        private final CountDownLatch release;
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();

        private ParallelModel(CountDownLatch bothEntered, CountDownLatch release) {
            this.bothEntered = bothEntered;
            this.release = release;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.defer(() -> {
                int now = active.incrementAndGet();
                maxActive.accumulateAndGet(now, Math::max);
                bothEntered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        return Flux.error(new AssertionError("parallel release timed out"));
                    }
                    return Flux.just(response("done"));
                }
                catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    return Flux.error(failure);
                }
                finally {
                    active.decrementAndGet();
                }
            });
        }

        @Override
        public String getModelName() {
            return "parallel";
        }
    }

    private static final class HitlThenReplyModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<ToolUseBlock> pending = new AtomicReference<>();
        private final List<String> agentKeys = new CopyOnWriteArrayList<>();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.deferContextual(context -> {
                RuntimeContext runtime = context.get(io.agentscope.core.agent.AgentBase.RUNTIME_CONTEXT_KEY);
                agentKeys.add(runtime.get(LiteFlowAgentContext.class).getAgentKey());
                if (calls.getAndIncrement() == 0) {
                    ToolUseBlock tool = new ToolUseBlock(
                            "hitl-tool",
                            "execute",
                            Map.of("command", "write hitl.txt approved"),
                            "{\"command\":\"write hitl.txt approved\"}",
                            Map.of(),
                            ToolCallState.PENDING);
                    pending.set(tool);
                    ContentBlock content = tool;
                    return Flux.just(ChatResponse.builder()
                            .content(List.of(content))
                            .finishReason("tool_calls")
                            .build());
                }
                return Flux.just(response("done"));
            });
        }

        @Override
        public String getModelName() {
            return "hitl-then-reply";
        }
    }

    private static final class CountingReplyModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            calls.incrementAndGet();
            return Flux.just(response("unused"));
        }

        @Override
        public String getModelName() {
            return "counting-reply";
        }
    }

    private static final class ToolThenReplyModel implements Model {
        private final String command;
        private final AtomicInteger calls = new AtomicInteger();

        private ToolThenReplyModel(String command) {
            this.command = command;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            if (calls.getAndIncrement() == 0) {
                assertTrue(tools.stream().anyMatch(tool -> "execute".equals(tool.getName())));
                ToolUseBlock tool = new ToolUseBlock(
                        "write-tool",
                        "execute",
                        Map.of("command", command),
                        "{\"command\":\"" + command + "\"}",
                        Map.of(),
                        ToolCallState.PENDING);
                ContentBlock content = tool;
                return Flux.just(ChatResponse.builder()
                        .content(List.of(content))
                        .finishReason("tool_calls")
                        .build());
            }
            return Flux.just(response("written"));
        }

        @Override
        public String getModelName() {
            return "snapshot-writer";
        }
    }

    private static final class ReadThenReplyModel implements Model {
        private final String command;
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<String> toolOutput = new AtomicReference<>();

        private ReadThenReplyModel(String command) {
            this.command = command;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            if (calls.getAndIncrement() == 0) {
                ToolUseBlock tool = new ToolUseBlock(
                        "read-tool",
                        "execute",
                        Map.of("command", command),
                        "{\"command\":\"" + command + "\"}",
                        Map.of(),
                        ToolCallState.PENDING);
                ContentBlock content = tool;
                return Flux.just(ChatResponse.builder()
                        .content(List.of(content))
                        .finishReason("tool_calls")
                        .build());
            }
            String observed = messages.stream()
                    .flatMap(message -> message.getContentBlocks(ToolResultBlock.class).stream())
                    .flatMap(result -> result.getOutput().stream())
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .map(TextBlock::getText)
                    .reduce("", String::concat);
            toolOutput.set(observed);
            return Flux.just(response("rebuilt"));
        }

        @Override
        public String getModelName() {
            return "snapshot-reader";
        }
    }

    private static final class RecordingGuard implements AgentInvocationGuard {
        private final AgentInvocationGuard delegate = new LocalAgentInvocationGuard();
        private final List<AgentInvocationKey> workspaceKeys = new CopyOnWriteArrayList<>();
        private final List<AgentInvocationKey> stateKeys = new CopyOnWriteArrayList<>();
        private final CountDownLatch secondWorkspaceAttempt = new CountDownLatch(1);

        @Override
        public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
            List<AgentInvocationKey> keys = key.scope() == AgentInvocationScope.WORKSPACE
                    ? workspaceKeys : stateKeys;
            keys.add(key);
            if (key.scope() == AgentInvocationScope.WORKSPACE && keys.size() == 2) {
                secondWorkspaceAttempt.countDown();
            }
            return delegate.acquire(key, timeout);
        }
    }

    private static final class FailingStateGuard implements AgentInvocationGuard {
        private final RuntimeException stateFailure;
        private final List<String> events = new CopyOnWriteArrayList<>();

        private FailingStateGuard(RuntimeException stateFailure) {
            this.stateFailure = stateFailure;
        }

        @Override
        public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
            events.add("acquire:" + key.scope());
            if (key.scope() == AgentInvocationScope.STATE) {
                throw stateFailure;
            }
            return new AgentInvocationLease() {
                @Override
                public AgentInvocationKey key() {
                    return key;
                }

                @Override
                public void close() {
                    events.add("close:" + key.scope());
                }
            };
        }
    }

    private static final class LifecycleRecordingGuard implements AgentInvocationGuard {
        private final AgentInvocationGuard delegate = new LocalAgentInvocationGuard();
        private final List<String> events = new CopyOnWriteArrayList<>();

        @Override
        public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
            events.add("acquire:" + key.scope());
            AgentInvocationLease lease = delegate.acquire(key, timeout);
            return new AgentInvocationLease() {
                @Override
                public AgentInvocationKey key() {
                    return key;
                }

                @Override
                public void close() {
                    lease.close();
                    events.add("close:" + key.scope());
                }
            };
        }
    }

    private static final class GuardContextAware extends LocalContextAware {
        private final AgentInvocationGuard guard;

        private GuardContextAware(AgentInvocationGuard guard) {
            this.guard = guard;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getBean(String name) {
            return "recording-guard".equals(name) ? (T) guard : null;
        }

        @Override
        public <T> Map<String, T> getBeansOfType(Class<T> type) {
            return Map.of();
        }
    }

    private record TestState(String value) implements State {
    }

    private static class RecordingStore implements AgentStateStore {
        private final Map<SlotKey, Map<String, State>> single = new LinkedHashMap<>();
        private final Map<SlotKey, Map<String, List<? extends State>>> lists =
                new LinkedHashMap<>();
        private final List<Call> calls = new ArrayList<>();

        @Override
        public synchronized void save(
                String userId, String sessionId, String key, State value) {
            single.computeIfAbsent(new SlotKey(userId, sessionId), ignored -> new LinkedHashMap<>())
                    .put(key, value);
            calls.add(new Call("save-one", userId, sessionId, key,
                    value instanceof TestState test ? test.value() : value.toString()));
        }

        @Override
        public synchronized void save(
                String userId, String sessionId, String key, List<? extends State> values) {
            lists.computeIfAbsent(new SlotKey(userId, sessionId), ignored -> new LinkedHashMap<>())
                    .put(key, List.copyOf(values));
            calls.add(new Call("save-list", userId, sessionId, key, values.toString()));
        }

        @Override
        public synchronized <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            State value = single.getOrDefault(new SlotKey(userId, sessionId), Map.of()).get(key);
            calls.add(new Call("get", userId, sessionId, key,
                    value instanceof TestState test ? test.value() : String.valueOf(value)));
            return Optional.ofNullable(value).map(type::cast);
        }

        @Override
        public synchronized <T extends State> List<T> getList(
                String userId, String sessionId, String key, Class<T> itemType) {
            List<? extends State> values = lists
                    .getOrDefault(new SlotKey(userId, sessionId), Map.of())
                    .getOrDefault(key, List.of());
            calls.add(new Call("get-list", userId, sessionId, key, values.toString()));
            return values.stream().map(itemType::cast).toList();
        }

        @Override
        public synchronized boolean exists(String userId, String sessionId) {
            SlotKey slot = new SlotKey(userId, sessionId);
            calls.add(new Call("exists", userId, sessionId, null, null));
            return single.containsKey(slot) || lists.containsKey(slot);
        }

        @Override
        public synchronized void delete(String userId, String sessionId) {
            SlotKey slot = new SlotKey(userId, sessionId);
            single.remove(slot);
            lists.remove(slot);
            calls.add(new Call("delete-session", userId, sessionId, null, null));
        }

        @Override
        public synchronized void delete(String userId, String sessionId, String key) {
            SlotKey slot = new SlotKey(userId, sessionId);
            Map<String, State> singleValues = single.get(slot);
            if (singleValues != null) {
                singleValues.remove(key);
            }
            Map<String, List<? extends State>> listValues = lists.get(slot);
            if (listValues != null) {
                listValues.remove(key);
            }
            calls.add(new Call("delete-key", userId, sessionId, key, null));
        }

        @Override
        public synchronized Set<String> listSessionIds(String userId) {
            Set<String> result = new LinkedHashSet<>();
            single.keySet().stream()
                    .filter(slot -> java.util.Objects.equals(userId, slot.userId()))
                    .map(SlotKey::sessionId)
                    .forEach(result::add);
            lists.keySet().stream()
                    .filter(slot -> java.util.Objects.equals(userId, slot.userId()))
                    .map(SlotKey::sessionId)
                    .forEach(result::add);
            calls.add(new Call("list", userId, null, null, null));
            return result;
        }

        synchronized List<Call> calls() {
            return List.copyOf(calls);
        }

        synchronized Call singleSave(String value) {
            return calls.stream()
                    .filter(call -> call.operation().equals("save-one"))
                    .filter(call -> value.equals(call.value()))
                    .findFirst()
                    .orElseThrow();
        }

        private record SlotKey(String userId, String sessionId) {
        }

        record Call(
                String operation, String userId, String sessionId, String key, String value) {
            Call withOperationAndValue(String nextOperation, String nextValue) {
                return new Call(nextOperation, userId, sessionId, key, nextValue);
            }
        }
    }

    private static final class FailingLoadStore extends RecordingStore {
        private final RuntimeException agentFailure;
        private final RuntimeException workspaceFailure;

        private FailingLoadStore(
                RuntimeException agentFailure, RuntimeException workspaceFailure) {
            this.agentFailure = agentFailure;
            this.workspaceFailure = workspaceFailure;
        }

        @Override
        public synchronized <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            if ("_sandbox_state".equals(key)) {
                throw workspaceFailure;
            }
            throw agentFailure;
        }
    }

    private static final class FailFirstSandboxLoadStore extends RecordingStore {
        private final RuntimeException failure;
        private final AtomicInteger sandboxLoadAttempts = new AtomicInteger();

        private FailFirstSandboxLoadStore(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public synchronized <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            if ("_sandbox_state".equals(key)
                    && sandboxLoadAttempts.incrementAndGet() == 1) {
                throw failure;
            }
            return super.get(userId, sessionId, key, type);
        }
    }
}
