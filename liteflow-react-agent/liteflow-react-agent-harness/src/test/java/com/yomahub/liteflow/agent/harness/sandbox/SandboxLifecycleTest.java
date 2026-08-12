package com.yomahub.liteflow.agent.harness.sandbox;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.harness.runtime.SandboxCallGate;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.hitl.AgentConfirmationHandler;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolResultBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.State;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.spec.SandboxFilesystemSpec;
import io.agentscope.harness.agent.sandbox.SandboxIsolationKey;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerFilesystemSpec;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SandboxLifecycleTest {

    @TempDir
    Path tempDir;

    @Test
    void successUsesTheRealHarnessLifecycleInUpstreamOrder() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        RecordingStateStore stateStore = new RecordingStateStore(events);
        InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
        FakeSandboxClient client = new FakeSandboxClient(events);
        HarnessAgent agent = agent(
                new ToolThenReplyModel("write result.txt persisted"),
                stateStore,
                snapshots,
                client,
                events);

        try {
            Msg reply = agent.call(
                            List.of(new UserMessage("write a file")), context("success"))
                    .block(Duration.ofSeconds(5));

            assertNotNull(reply);
            assertEquals(
                    List.of(
                            "lease-acquire",
                            "acquire",
                            "start",
                            "tool",
                            "persist-state",
                            "stop",
                            "shutdown",
                            "lease-close"),
                    lifecycle(events),
                    events.toString());
            assertEquals(1, snapshots.snapshotCount());
        }
        finally {
            agent.close();
        }
    }

    @Test
    void errorAndCancellationStillPersistReleaseAndCloseTheLease() throws Exception {
        for (boolean cancel : List.of(false, true)) {
            List<String> events = new CopyOnWriteArrayList<>();
            RecordingStateStore stateStore = new RecordingStateStore(events);
            InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
            FakeSandboxClient client = new FakeSandboxClient(events);
            CountDownLatch modelEntered = new CountDownLatch(1);
            Model model = cancel
                    ? new NeverModel(modelEntered)
                    : new ErrorModel(modelEntered);
            HarnessAgent agent = agent(model, stateStore, snapshots, client, events);
            try {
                Mono<Msg> call = agent.call(
                        List.of(new UserMessage("terminate")), context("terminal-" + cancel));
                if (cancel) {
                    Disposable subscription = call.subscribe(
                            ignored -> { }, ignored -> { });
                    assertTrue(modelEntered.await(5, TimeUnit.SECONDS));
                    subscription.dispose();
                }
                else {
                    assertThrows(RuntimeException.class,
                            () -> call.block(Duration.ofSeconds(5)));
                }

                assertEquals(
                        List.of(
                                "lease-acquire",
                                "acquire",
                                "start",
                                "persist-state",
                                "stop",
                                "shutdown",
                                "lease-close"),
                        lifecycle(events),
                        "terminal path=" + (cancel ? "cancel" : "error"));
            }
            finally {
                agent.close();
            }
        }
    }

    @Test
    void hitlContinuationUsesTwoCompletePublicHarnessLifecycleRounds() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        RecordingStateStore stateStore = new RecordingStateStore(events);
        InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
        FakeSandboxClient client = new FakeSandboxClient(events);
        ToolThenReplyModel model = new ToolThenReplyModel("write approved.txt yes");
        HarnessAgent agent = agent(model, stateStore, snapshots, client, events,
                PermissionContextState.builder()
                        .addAskRule("execute", new PermissionRule(
                                "execute", null, PermissionBehavior.ASK, "test"))
                        .build());
        RuntimeContext context = context("hitl");

        try {
            Msg asking = agent.call(
                            List.of(new UserMessage("ask first")), context)
                    .block(Duration.ofSeconds(5));
            assertNotNull(asking);
            assertEquals(GenerateReason.PERMISSION_ASKING, asking.getGenerateReason());
            ToolUseBlock pending = asking.getContentBlocks(ToolUseBlock.class).get(0);

            Msg completed = agent.call(
                            List.of(UserMessage.builder()
                                    .metadata(Map.of(
                                            Msg.METADATA_CONFIRM_RESULTS,
                                            List.of(new ConfirmResult(true, pending))))
                                    .build()),
                            context)
                    .block(Duration.ofSeconds(5));

            assertNotNull(completed);
            assertEquals(
                    List.of(
                            "lease-acquire", "acquire", "start", "persist-state", "stop",
                            "shutdown", "lease-close",
                            "lease-acquire", "acquire", "start", "tool", "persist-state",
                            "stop", "shutdown", "lease-close"),
                    lifecycle(events));
        }
        finally {
            agent.close();
        }
    }

    @Test
    void snapshotRestoresFilesAfterTheOldAgentIsClosedAndRebuilt() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        RecordingStateStore stateStore = new RecordingStateStore(events);
        InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
        FakeSandboxClient firstClient = new FakeSandboxClient(events);
        HarnessAgent first = agent(
                new ToolThenReplyModel("write durable.txt from-snapshot"),
                stateStore,
                snapshots,
                firstClient,
                events);
        first.call(List.of(new UserMessage("write")), context("snapshot"))
                .block(Duration.ofSeconds(5));
        first.close();

        assertNull(firstClient.latestSandbox().file("durable.txt"),
                "shutdown must destroy the old runtime workspace");

        FakeSandboxClient secondClient = new FakeSandboxClient(events);
        ReadThenReplyModel reader = new ReadThenReplyModel("read durable.txt");
        HarnessAgent rebuilt = agent(
                reader, stateStore, snapshots, secondClient, events);
        try {
            rebuilt.call(List.of(new UserMessage("restore")), context("snapshot"))
                    .block(Duration.ofSeconds(5));

            assertTrue(reader.toolOutput.get().contains("from-snapshot"), events.toString());
            assertNull(secondClient.latestSandbox().file("durable.txt"),
                    "the rebuilt runtime must also be destroyed after its public call");
            assertTrue(events.stream().anyMatch(event -> event.startsWith("snapshot-restore:")));
            assertEquals(
                    firstClient.latestSandbox().getState().getSessionId(),
                    secondClient.latestSandbox().getState().getSessionId());
        }
        finally {
            rebuilt.close();
        }
    }

    @Test
    void sandboxCallGateIsFifoAndReleasesOnCompleteErrorAndHolderCancellation() {
        SandboxCallGate gate = new SandboxCallGate();
        List<String> entered = new CopyOnWriteArrayList<>();
        Sinks.One<String> firstResult = Sinks.one();

        Mono<String> first = gate.execute(() -> {
            entered.add("first");
            return firstResult.asMono();
        });
        Mono<String> second = gate.execute(() -> {
            entered.add("second");
            return Mono.error(new IllegalStateException("second failed"));
        });
        Mono<String> third = gate.execute(() -> {
            entered.add("third");
            return Mono.just("third");
        });

        Disposable firstSubscription = first.subscribe();
        AtomicReference<Throwable> secondFailure = new AtomicReference<>();
        second.subscribe(ignored -> { }, secondFailure::set);
        AtomicReference<String> thirdValue = new AtomicReference<>();
        third.subscribe(thirdValue::set);
        assertEquals(List.of("first"), entered);

        firstResult.tryEmitValue("first");

        assertInstanceOf(IllegalStateException.class, secondFailure.get());
        assertEquals("third", thirdValue.get());
        assertEquals(List.of("first", "second", "third"), entered);

        Sinks.One<String> held = Sinks.one();
        Disposable holder = gate.execute(() -> held.asMono()).subscribe();
        AtomicReference<String> afterCancel = new AtomicReference<>();
        gate.execute(() -> Mono.just("released")).subscribe(afterCancel::set);
        holder.dispose();
        assertEquals("released", afterCancel.get());
        firstSubscription.dispose();
        gate.close();
    }

    @Test
    void sandboxCallGateRemovesCancelledWaitersAndCloseRejectsTheQueue() {
        SandboxCallGate gate = new SandboxCallGate();
        Sinks.One<String> held = Sinks.one();
        Disposable holder = gate.execute(() -> held.asMono()).subscribe();
        AtomicInteger cancelledSourceCalls = new AtomicInteger();
        Disposable waiter = gate.execute(() -> {
            cancelledSourceCalls.incrementAndGet();
            return Mono.just("must-not-run");
        }).subscribe();
        waiter.dispose();

        AtomicReference<Throwable> closeFailure = new AtomicReference<>();
        gate.execute(() -> Mono.just("queued"))
                .subscribe(ignored -> { }, closeFailure::set);
        gate.close();

        assertEquals(0, cancelledSourceCalls.get());
        assertInstanceOf(IllegalStateException.class, closeFailure.get());
        assertThrows(IllegalStateException.class,
                () -> gate.execute(() -> Mono.just("late")).block(Duration.ofSeconds(1)));
        holder.dispose();
    }

    @Test
    void separateSandboxCallGatesDoNotSerializeDifferentRuntimes() throws Exception {
        SandboxCallGate firstGate = new SandboxCallGate();
        SandboxCallGate secondGate = new SandboxCallGate();
        CountDownLatch entered = new CountDownLatch(2);
        Sinks.One<String> firstRelease = Sinks.one();
        Sinks.One<String> secondRelease = Sinks.one();

        Disposable first = firstGate.execute(() -> {
            entered.countDown();
            return firstRelease.asMono();
        }).subscribe();
        Disposable second = secondGate.execute(() -> {
            entered.countDown();
            return secondRelease.asMono();
        }).subscribe();

        assertTrue(entered.await(5, TimeUnit.SECONDS));
        firstRelease.tryEmitValue("first");
        secondRelease.tryEmitValue("second");
        first.dispose();
        second.dispose();
        firstGate.close();
        secondGate.close();
    }

    @Test
    void dockerComponentSerializesDifferentSessionsAcrossTheWholePublicInvocation()
            throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
        FakeSandboxClient client = new FakeSandboxClient(events);
        Sinks.One<ChatResponse> firstRelease = Sinks.one();
        Sinks.One<Void> secondProceed = Sinks.one();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch secondSubscribed = new CountDownLatch(1);
        SessionBlockingModel model = new SessionBlockingModel(firstEntered, firstRelease);
        DockerComponent component = dockerComponent(
                model, client, snapshots, secondProceed, secondSubscribed, null, null);
        Slot firstSlot = slot("conversation-a", "request-a");
        Slot secondSlot = slot("conversation-b", "request-b");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> first = executor.submit(() -> component.process(firstSlot));
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            Future<?> second = executor.submit(() -> component.process(secondSlot));
            assertTrue(secondSubscribed.await(5, TimeUnit.SECONDS));

            secondProceed.tryEmitEmpty();
            assertEquals(1, model.sessions.size(),
                    "the second session must wait outside the shared Harness middleware");

            firstRelease.tryEmitValue(response(
                    TextBlock.builder().text("first").build(), "stop"));
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);

            assertEquals(2, model.sessions.stream().distinct().count());
            assertEquals(
                    List.of(
                            "acquire", "start", "stop", "shutdown",
                            "acquire", "start", "stop", "shutdown"),
                    lifecycle(events));
        }
        finally {
            firstRelease.tryEmitEmpty();
            secondProceed.tryEmitEmpty();
            executor.shutdownNow();
            component.close();
            LiteflowConfigGetter.clean();
        }
    }

    @Test
    void dockerComponentKeepsTheGateAcrossHitlContinuationRounds() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        InMemorySandboxSnapshot snapshots = new InMemorySandboxSnapshot(events);
        FakeSandboxClient client = new FakeSandboxClient(events);
        Sinks.One<List<ConfirmResult>> confirmation = Sinks.one();
        CountDownLatch handlerEntered = new CountDownLatch(1);
        Sinks.One<Void> secondProceed = Sinks.one();
        CountDownLatch secondSubscribed = new CountDownLatch(1);
        SessionHitlModel model = new SessionHitlModel();
        AgentConfirmationHandler handler = (event, context) -> {
            handlerEntered.countDown();
            return confirmation.asMono();
        };
        DockerComponent component = dockerComponent(
                model, client, snapshots, secondProceed, secondSubscribed, handler,
                PermissionContextState.builder()
                        .addAskRule("execute", new PermissionRule(
                                "execute", null, PermissionBehavior.ASK, "test"))
                        .build());
        Slot hitlSlot = slot("conversation-hitl", "request-hitl");
        Slot otherSlot = slot("conversation-other", "request-other");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> hitl = executor.submit(() -> component.process(hitlSlot));
            assertTrue(handlerEntered.await(5, TimeUnit.SECONDS));
            Future<?> other = executor.submit(() -> component.process(otherSlot));
            assertTrue(secondSubscribed.await(5, TimeUnit.SECONDS));

            secondProceed.tryEmitEmpty();
            assertEquals(1, lifecycle(events).stream()
                    .filter("acquire"::equals)
                    .count(), "another session must not interleave while confirmation is pending");

            ToolUseBlock pending = model.pending.get();
            assertNotNull(pending);
            confirmation.tryEmitValue(List.of(new ConfirmResult(true, pending)));
            hitl.get(5, TimeUnit.SECONDS);
            other.get(5, TimeUnit.SECONDS);

            assertEquals(
                    List.of(
                            "acquire", "start", "stop", "shutdown",
                            "acquire", "start", "tool", "stop", "shutdown",
                            "acquire", "start", "stop", "shutdown"),
                    lifecycle(events));
        }
        finally {
            confirmation.tryEmitEmpty();
            secondProceed.tryEmitEmpty();
            executor.shutdownNow();
            component.close();
            LiteflowConfigGetter.clean();
        }
    }

    @Test
    void closingTheDockerRuntimeClosesItsGate() throws Exception {
        List<String> events = new CopyOnWriteArrayList<>();
        Sinks.One<Void> unusedProceed = Sinks.one();
        DockerComponent component = dockerComponent(
                new ReplyModel("built"),
                new FakeSandboxClient(events),
                new InMemorySandboxSnapshot(events),
                unusedProceed,
                new CountDownLatch(1),
                null,
                null);
        try {
            component.process(slot("close-session", "close-request"));
            HarnessAgentRuntime runtime = component.runtime;
            assertNotNull(runtime);

            component.close();

            IllegalStateException failure = assertThrows(
                    IllegalStateException.class,
                    () -> runtime.executeSandboxCall(() -> Mono.just("late"))
                            .block(Duration.ofSeconds(1)));
            assertTrue(failure.getMessage().contains("closed"));
        }
        finally {
            component.close();
            LiteflowConfigGetter.clean();
        }
    }

    private HarnessAgent agent(
            Model model,
            RecordingStateStore stateStore,
            InMemorySandboxSnapshot snapshots,
            FakeSandboxClient client,
            List<String> events) throws Exception {
        return agent(model, stateStore, snapshots, client, events, null);
    }

    private HarnessAgent agent(
            Model model,
            RecordingStateStore stateStore,
            InMemorySandboxSnapshot snapshots,
            FakeSandboxClient client,
            List<String> events,
            PermissionContextState permissionContext) throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("workspace"));
        AgentConfig config = new AgentConfig();
        config.getWorkspace().setRoot(workspace.toString());
        HarnessFilesystemContext filesystemContext = new HarnessFilesystemContext(
                workspace, 1024L, Duration.ofSeconds(5), config);
        HarnessAgent.Builder builder = HarnessAgent.builder()
                .name("sandbox-lifecycle")
                .agentId("sandbox-lifecycle")
                .model(model)
                .stateStore(stateStore)
                .workspace(workspace)
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
        builder.permissionContext(permissionContext == null
                ? PermissionContextState.builder()
                        .addAllowRule("execute", new PermissionRule(
                                "execute", null, PermissionBehavior.ALLOW, "test"))
                        .build()
                : permissionContext);
        new DockerSandboxConfigurer(ignored -> snapshots, client)
                .configure(builder, filesystemContext);
        configuredSandboxSpec(builder).executionGuard(key -> {
            assertInstanceOf(SandboxIsolationKey.class, key);
            events.add("lease-acquire");
            return () -> events.add("lease-close");
        });
        return builder.build();
    }

    private DockerComponent dockerComponent(
            Model model,
            FakeSandboxClient client,
            InMemorySandboxSnapshot snapshots,
            Sinks.One<Void> secondProceed,
            CountDownLatch secondSubscribed,
            AgentConfirmationHandler confirmationHandler,
            PermissionContextState permissionContext) throws Exception {
        Path workspace = Files.createDirectories(tempDir.resolve("component-workspace"));
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace("sandbox-component-test");
        agent.getRuntime().setDefaultUserId("test-user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(5));
        agent.getWorkspace().setRoot(workspace.toString());
        agent.getHarness().setFilesystemBackend(HarnessFilesystemBackend.DOCKER);
        LiteflowConfig liteflow = new LiteflowConfig();
        liteflow.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(liteflow);

        DockerComponent component = new DockerComponent(
                model,
                client,
                ignored -> snapshots,
                secondProceed,
                secondSubscribed,
                confirmationHandler,
                permissionContext);
        component.setNodeId("sandbox-component");
        return component;
    }

    private static Slot slot(String conversationId, String requestId) {
        Slot slot = new Slot();
        slot.setChainId("sandbox-chain");
        slot.setConversationId(conversationId);
        slot.putRequestId(requestId);
        return slot;
    }

    private static SandboxFilesystemSpec configuredSandboxSpec(HarnessAgent.Builder builder)
            throws Exception {
        Field field = HarnessAgent.Builder.class.getDeclaredField("sandboxFilesystemSpec");
        assertTrue(field.trySetAccessible());
        return assertInstanceOf(DockerFilesystemSpec.class, field.get(builder));
    }

    private static RuntimeContext context(String sessionId) {
        return RuntimeContext.builder().userId("user").sessionId(sessionId).build();
    }

    private static List<String> lifecycle(List<String> events) {
        return events.stream()
                .map(event -> event.contains(":")
                        ? event.substring(0, event.indexOf(':'))
                        : event)
                .filter(event -> List.of(
                                "lease-acquire",
                                "acquire",
                                "start",
                                "tool",
                                "persist-state",
                                "stop",
                                "shutdown",
                                "lease-close")
                        .contains(event))
                .toList();
    }

    private static ChatResponse response(ContentBlock block, String finishReason) {
        return ChatResponse.builder().content(List.of(block)).finishReason(finishReason).build();
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
                assertTrue(tools.stream().anyMatch(tool -> "execute".equals(tool.getName())),
                        tools.toString());
                ToolUseBlock tool = new ToolUseBlock(
                        "tool-1",
                        "execute",
                        Map.of("command", command),
                        "{\"command\":\"" + command + "\"}",
                        Map.of(),
                        ToolCallState.PENDING);
                return Flux.just(response(tool, "tool_calls"));
            }
            return Flux.just(response(TextBlock.builder().text("done").build(), "stop"));
        }

        @Override
        public String getModelName() {
            return "tool-then-reply";
        }
    }

    private static final class ReplyModel implements Model {
        private final String reply;

        private ReplyModel(String reply) {
            this.reply = reply;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.just(response(TextBlock.builder().text(reply).build(), "stop"));
        }

        @Override
        public String getModelName() {
            return "reply";
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
                return Flux.just(response(tool, "tool_calls"));
            }
            String observed = messages.stream()
                    .flatMap(message -> message.getContentBlocks(ToolResultBlock.class).stream())
                    .flatMap(result -> result.getOutput().stream())
                    .filter(TextBlock.class::isInstance)
                    .map(TextBlock.class::cast)
                    .map(TextBlock::getText)
                    .reduce("", (left, right) -> left + right);
            toolOutput.set(observed);
            return Flux.just(response(TextBlock.builder().text("rebuilt").build(), "stop"));
        }

        @Override
        public String getModelName() {
            return "read-then-reply";
        }
    }

    private static final class ErrorModel implements Model {
        private final CountDownLatch entered;

        private ErrorModel(CountDownLatch entered) {
            this.entered = entered;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            entered.countDown();
            return Flux.error(new IllegalStateException("model failed"));
        }

        @Override
        public String getModelName() {
            return "error";
        }
    }

    private static final class NeverModel implements Model {
        private final CountDownLatch entered;

        private NeverModel(CountDownLatch entered) {
            this.entered = entered;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            entered.countDown();
            return Flux.never();
        }

        @Override
        public String getModelName() {
            return "never";
        }
    }

    private static final class SessionBlockingModel implements Model {
        private final CountDownLatch firstEntered;
        private final Sinks.One<ChatResponse> firstRelease;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<String> sessions = new CopyOnWriteArrayList<>();

        private SessionBlockingModel(
                CountDownLatch firstEntered,
                Sinks.One<ChatResponse> firstRelease) {
            this.firstEntered = firstEntered;
            this.firstRelease = firstRelease;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.deferContextual(context -> {
                RuntimeContext runtime = context.get(AgentBase.RUNTIME_CONTEXT_KEY);
                sessions.add(runtime.getSessionId());
                if (calls.getAndIncrement() == 0) {
                    firstEntered.countDown();
                    return firstRelease.asMono().flux();
                }
                return Flux.just(response(
                        TextBlock.builder().text("second").build(), "stop"));
            });
        }

        @Override
        public String getModelName() {
            return "session-blocking";
        }
    }

    private static final class SessionHitlModel implements Model {
        private final AtomicInteger calls = new AtomicInteger();
        private final AtomicReference<ToolUseBlock> pending = new AtomicReference<>();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.defer(() -> {
                int call = calls.getAndIncrement();
                if (call == 0) {
                    ToolUseBlock tool = new ToolUseBlock(
                            "hitl-tool",
                            "execute",
                            Map.of("command", "write hitl.txt approved"),
                            "{\"command\":\"write hitl.txt approved\"}",
                            Map.of(),
                            ToolCallState.PENDING);
                    pending.set(tool);
                    return Flux.just(response(tool, "tool_calls"));
                }
                return Flux.just(response(
                        TextBlock.builder().text("done").build(), "stop"));
            });
        }

        @Override
        public String getModelName() {
            return "session-hitl";
        }
    }

    private static final class DockerComponent extends HarnessAgentComponent {
        private final ThreadLocal<Slot> invocationSlot = new ThreadLocal<>();
        private final Model model;
        private final FakeSandboxClient client;
        private final SandboxSnapshotProvider snapshots;
        private final Sinks.One<Void> secondProceed;
        private final CountDownLatch secondSubscribed;
        private final AgentConfirmationHandler confirmationHandler;
        private final PermissionContextState permissionContext;
        private final AtomicInteger invocations = new AtomicInteger();
        private volatile HarnessAgentRuntime runtime;

        private DockerComponent(
                Model model,
                FakeSandboxClient client,
                SandboxSnapshotProvider snapshots,
                Sinks.One<Void> secondProceed,
                CountDownLatch secondSubscribed,
                AgentConfirmationHandler confirmationHandler,
                PermissionContextState permissionContext) {
            this.model = model;
            this.client = client;
            this.snapshots = snapshots;
            this.secondProceed = secondProceed;
            this.secondSubscribed = secondSubscribed;
            this.confirmationHandler = confirmationHandler;
            this.permissionContext = permissionContext;
        }

        private void process(Slot slot) {
            invocationSlot.set(slot);
            try {
                process();
            }
            catch (Exception failure) {
                throw new RuntimeException(failure);
            }
            finally {
                invocationSlot.remove();
            }
        }

        @Override
        public Slot getSlot() {
            return invocationSlot.get();
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
        protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            HarnessAgentRuntime built = super.buildRuntime(buildContext);
            runtime = built;
            return built;
        }

        @Override
        protected SandboxClient<DockerSandboxClientOptions> dockerSandboxClient() {
            return client;
        }

        @Override
        protected SandboxSnapshotProvider sandboxSnapshotProvider() {
            return snapshots;
        }

        @Override
        protected AgentConfirmationHandler confirmationHandler() {
            return confirmationHandler;
        }

        @Override
        protected PermissionContextState permissionContext() {
            return permissionContext == null
                    ? PermissionContextState.builder().build()
                    : permissionContext;
        }

        @Override
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
            return builder
                    .disableSubagents()
                    .disableCompaction()
                    .disableToolResultEviction()
                    .disableMemoryTools()
                    .disableMemoryHooks()
                    .disableWorkspaceContext()
                    .disableAtPathExpansion()
                    .disableDefaultWorkspaceSkills()
                    .disableDynamicSkills()
                    .disableToolsConfig()
                    .disableFilesystemTools();
        }

        @Override
        protected Mono<Msg> invokeRuntime(
                HarnessAgentRuntime runtime,
                List<Msg> input,
                AgentOutputSpec output,
                RuntimeContext runtimeContext,
                LiteFlowAgentContext liteflowContext) {
            Mono<Msg> invocation = super.invokeRuntime(
                    runtime, input, output, runtimeContext, liteflowContext);
            if (invocations.incrementAndGet() != 2) {
                return invocation;
            }
            return secondProceed.asMono()
                    .doOnSubscribe(ignored -> secondSubscribed.countDown())
                    .then(invocation);
        }

        @Override
        protected String systemPrompt() {
            return "Answer deterministically.";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "test";
        }
    }

    private static final class RecordingStateStore extends InMemoryAgentStateStore {
        private final List<String> events;

        private RecordingStateStore(List<String> events) {
            this.events = events;
        }

        @Override
        public void save(String userId, String sessionId, String key, State value) {
            if ("_sandbox_state".equals(key)) {
                events.add("persist-state");
            }
            super.save(userId, sessionId, key, value);
        }

        @Override
        public <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            return super.get(userId, sessionId, key, type);
        }
    }
}
