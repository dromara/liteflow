package com.yomahub.liteflow.agent.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.InvocationIdentityResolver;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.event.AgentFlowEventData;
import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.guard.AgentInvocationScope;
import com.yomahub.liteflow.agent.guard.LocalAgentInvocationGuard;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.middleware.StateStoreFailureMiddleware;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.AgentRuntime;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import com.yomahub.liteflow.property.agent.AgentStateStoreFailurePolicy;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;
import com.yomahub.liteflow.flow.FlowEvent;
import com.yomahub.liteflow.flow.FlowEventPublisher;
import io.agentscope.core.agent.AgentBase;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.tool.Tool;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import reactor.core.publisher.Flux;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentCoreContractTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static Object originalLiteflowConfig;
    private static Object originalContextAware;
    private static LiteflowConfig liteflowConfigSentinel;
    private static LocalContextAware contextAwareSentinel;
    private final List<ContractComponent> components = new ArrayList<>();
    private Object previousLiteflowConfig;
    private Object previousContextAware;

    @TempDir
    Path tempDir;

    @BeforeAll
    static void installGlobalSentinels() throws Exception {
        Field configField = staticField(LiteflowConfigGetter.class, "liteflowConfig");
        Field contextField = staticField(ContextAwareHolder.class, "contextAware");
        originalLiteflowConfig = configField.get(null);
        originalContextAware = contextField.get(null);
        liteflowConfigSentinel = new LiteflowConfig();
        contextAwareSentinel = new LocalContextAware();
        configField.set(null, liteflowConfigSentinel);
        contextField.set(null, contextAwareSentinel);
    }

    @AfterAll
    static void verifyAndRestoreGlobalSentinels() throws Exception {
        Field configField = staticField(LiteflowConfigGetter.class, "liteflowConfig");
        Field contextField = staticField(ContextAwareHolder.class, "contextAware");
        try {
            assertSame(liteflowConfigSentinel, configField.get(null));
            assertSame(contextAwareSentinel, contextField.get(null));
        } finally {
            configField.set(null, originalLiteflowConfig);
            contextField.set(null, originalContextAware);
        }
    }

    @BeforeEach
    void preserveGlobals() throws Exception {
        previousLiteflowConfig = staticField(
                LiteflowConfigGetter.class, "liteflowConfig").get(null);
        previousContextAware = staticField(
                ContextAwareHolder.class, "contextAware").get(null);
    }

    @AfterEach
    void cleanUp() throws Exception {
        try {
            components.forEach(ContractComponent::close);
        } finally {
            staticField(LiteflowConfigGetter.class, "liteflowConfig")
                    .set(null, previousLiteflowConfig);
            staticField(ContextAwareHolder.class, "contextAware")
                    .set(null, previousContextAware);
        }
    }

    @Test
    void plainTextCallsReuseOneRealRuntimeAndExposeFreshInvocationContexts() throws Exception {
        configureAgent("core-contract");

        Slot slot = new TrackingSlot();
        slot.setChainId("contract-chain");
        slot.setConversationId("contract-conversation");
        slot.putRequestId("contract-request");
        slot.setAttachment("contract-unrelated", "keep");
        RecordingModel model = new RecordingModel("deterministic reply");
        ContractComponent component = new ContractComponent(slot, model);
        component.setNodeId("contract-agent");
        components.add(component);

        processAndAssertAttachments(component);
        AgentRuntime firstRuntime = component.runtime;
        AgentBase firstAgent = component.runtime.agent();
        processAndAssertAttachments(component);

        assertEquals("deterministic reply", slot.getResponseData());
        assertEquals(2, model.calls.get());
        assertEquals(1, component.runtimeBuilds.get());
        assertSame(firstRuntime, component.runtime);
        assertSame(firstAgent, component.runtime.agent());
        RuntimeContext firstRuntimeContext = model.contexts.get(0);
        RuntimeContext secondRuntimeContext = model.contexts.get(1);
        assertNotSame(firstRuntimeContext, secondRuntimeContext);
        LiteFlowAgentContext firstInvocation = firstRuntimeContext.get(LiteFlowAgentContext.class);
        LiteFlowAgentContext secondInvocation = secondRuntimeContext.get(LiteFlowAgentContext.class);
        assertNotSame(firstInvocation, secondInvocation);
        assertFalse(firstInvocation.getAttachmentKey().equals(secondInvocation.getAttachmentKey()));
        assertEquals("contract-user", firstRuntimeContext.getUserId());
        assertEquals(firstRuntimeContext.getSessionId(), secondRuntimeContext.getSessionId());
        assertSame(slot, firstRuntimeContext.get(Slot.class));
    }

    @Test
    void realOutputModesAndDistinctComponentsKeepRuntimePromptToolkitModelAndStateIsolated()
            throws Exception {
        configureAgent("output-isolation");

        ContractComponent text = component("text-conversation",
                new RecordingModel("plain reply"));
        text.systemPrompt = "alpha system";
        text.userPrompt = "alpha question";
        text.tools = List.of(new AlphaTool());
        processAndAssertAttachments(text);
        assertEquals("plain reply", text.slot.getResponseData());

        ContractComponent typed = component("typed-conversation",
                new RecordingModel("{\"answer\":\"typed reply\"}", true));
        typed.outputType = StructuredReply.class;
        processAndAssertAttachments(typed);
        StructuredReply typedReply = typed.slot.getResponseData();
        assertEquals("typed reply", typedReply.answer);

        JsonNode schema = schema();
        ContractComponent schemaComponent = component("schema-conversation",
                new RecordingModel("{\"answer\":\"schema reply\"}", true));
        schemaComponent.outputSchema = schema;
        processAndAssertAttachments(schemaComponent);
        JsonNode schemaReply = schemaComponent.slot.getResponseData();
        assertEquals("schema reply", schemaReply.path("answer").asText());
        LiteFlowAgentContext schemaContext = schemaComponent.model.contexts.get(0)
                .get(LiteFlowAgentContext.class);
        assertEquals(schema, schemaContext.getOutputSpec().jsonSchema());
        JsonNode responseFormatSchema = OBJECT_MAPPER.valueToTree(
                schemaComponent.model.options.get(0)
                        .getResponseFormat().getJsonSchema().getSchema());
        assertEquals(schema, responseFormatSchema);

        ContractComponent beta = component("beta-conversation",
                new RecordingModel("beta reply"));
        beta.systemPrompt = "beta system";
        beta.userPrompt = "beta question";
        beta.tools = List.of(new BetaTool());
        processAndAssertAttachments(beta);

        assertNotSame(text.runtime, beta.runtime);
        assertNotSame(text.runtime.agent(), beta.runtime.agent());
        assertNotSame(text.runtime.agent().getToolkit(), beta.runtime.agent().getToolkit());
        assertNotSame(text.runtime.stateStore(), beta.runtime.stateStore());
        assertNotSame(text.model, beta.model);
        assertEquals(List.of("alpha_tool"), text.model.toolNames.get(0));
        assertEquals(List.of("beta_tool"), beta.model.toolNames.get(0));
        String alphaSystemPrompt = systemPrompt(text.model.inputs.get(0));
        String betaSystemPrompt = systemPrompt(beta.model.inputs.get(0));
        assertTrue(alphaSystemPrompt.contains("alpha system"));
        assertFalse(alphaSystemPrompt.contains("beta system"));
        assertTrue(betaSystemPrompt.contains("beta system"));
        assertFalse(betaSystemPrompt.contains("alpha system"));
        assertTrue(text.model.inputs.get(0).stream()
                .noneMatch(message -> "beta question".equals(message.getTextContent())));
        assertTrue(beta.model.inputs.get(0).stream()
                .noneMatch(message -> "alpha question".equals(message.getTextContent())));
    }

    @Test
    void stateKeysSerializeTheSameIdentityWhileDifferentKeysOverlap() throws Exception {
        AgentConfig config = configureAgent("state-guard-contract");
        AttemptGuard guard = installGuard(config);
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ContractComponent first = component("shared-conversation",
                new RecordingModel("first", new BlockingPlan(firstEntered, releaseFirst)));
        ContractComponent second = component("shared-conversation",
                new RecordingModel("second"));
        first.agentKey = "shared-agent";
        second.agentKey = "shared-agent";
        Set<String> firstAttachments = attachmentSnapshot(first.slot);
        Set<String> secondAttachments = attachmentSnapshot(second.slot);
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            Future<?> firstCall = executor.submit(() -> {
                first.process();
                return null;
            });
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            Future<?> secondCall = executor.submit(() -> {
                second.process();
                return null;
            });
            assertTrue(guard.secondStateAttempt.await(5, TimeUnit.SECONDS));
            assertEquals(0, second.model.calls.get());
            releaseFirst.countDown();
            firstCall.get(5, TimeUnit.SECONDS);
            secondCall.get(5, TimeUnit.SECONDS);
            assertEquals(guard.stateKeys.get(0), guard.stateKeys.get(1));
            assertEquals(firstAttachments, attachmentSnapshot(first.slot));
            assertEquals(secondAttachments, attachmentSnapshot(second.slot));

            AttemptGuard overlapGuard = installGuard(config);
            ActiveBarrier barrier = new ActiveBarrier(2);
            ContractComponent third = component("conversation-three",
                    new RecordingModel("third", barrier));
            ContractComponent fourth = component("conversation-four",
                    new RecordingModel("fourth", barrier));
            third.agentKey = "agent-three";
            fourth.agentKey = "agent-four";
            Set<String> thirdAttachments = attachmentSnapshot(third.slot);
            Set<String> fourthAttachments = attachmentSnapshot(fourth.slot);
            Future<?> thirdCall = executor.submit(() -> {
                third.process();
                return null;
            });
            Future<?> fourthCall = executor.submit(() -> {
                fourth.process();
                return null;
            });
            assertTrue(barrier.entered.await(5, TimeUnit.SECONDS));
            assertEquals(2, barrier.maxActive.get());
            barrier.release.countDown();
            thirdCall.get(5, TimeUnit.SECONDS);
            fourthCall.get(5, TimeUnit.SECONDS);
            assertFalse(overlapGuard.stateKeys.get(0).equals(overlapGuard.stateKeys.get(1)));
            assertEquals(thirdAttachments, attachmentSnapshot(third.slot));
            assertEquals(fourthAttachments, attachmentSnapshot(fourth.slot));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void workspaceKeyExcludesAgentKeyAndSerializesWritesAcrossComponents() throws Exception {
        AgentConfig config = configureAgent("workspace-guard-contract");
        AttemptGuard guard = installGuard(config);
        Path journal = tempDir.resolve("workspace-journal.txt");
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch releaseFirst = new CountDownLatch(1);
        ContractComponent first = component("shared-workspace",
                new RecordingModel("first", new BlockingPlan(
                        firstEntered, releaseFirst, () -> append(journal, "agent-a"))));
        ContractComponent second = component("shared-workspace",
                new RecordingModel("second", new ActionPlan(() -> append(journal, "agent-b"))));
        first.agentKey = "agent-a";
        second.agentKey = "agent-b";
        first.workspaceLease = true;
        second.workspaceLease = true;
        Set<String> firstAttachments = attachmentSnapshot(first.slot);
        Set<String> secondAttachments = attachmentSnapshot(second.slot);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstCall = executor.submit(() -> {
                first.process();
                return null;
            });
            assertTrue(firstEntered.await(5, TimeUnit.SECONDS));
            Future<?> secondCall = executor.submit(() -> {
                second.process();
                return null;
            });
            assertTrue(guard.secondWorkspaceAttempt.await(5, TimeUnit.SECONDS));
            assertEquals(0, second.model.calls.get());
            releaseFirst.countDown();
            firstCall.get(5, TimeUnit.SECONDS);
            secondCall.get(5, TimeUnit.SECONDS);

            assertEquals(List.of("agent-a", "agent-b"), Files.readAllLines(journal));
            assertEquals(guard.workspaceKeys.get(0), guard.workspaceKeys.get(1));
            assertFalse(guard.stateKeys.get(0).equals(guard.stateKeys.get(1)));
            assertEquals(firstAttachments, attachmentSnapshot(first.slot));
            assertEquals(secondAttachments, attachmentSnapshot(second.slot));
        } finally {
            releaseFirst.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void jsonStoreCloseAndRecreateRestoresOnlyTheMatchingAgentState() throws Exception {
        Path stateRoot = tempDir.resolve("json-state");
        AgentConfig config = configureAgent("json-persistence-contract");
        config.getStateStore().setType(AgentStateStoreType.JSON);
        config.getStateStore().setJsonRoot(stateRoot.toString());

        ContractComponent alpha = component("persistent-conversation",
                new RecordingModel("alpha reply"));
        alpha.agentKey = "alpha-agent";
        alpha.userPrompt = "alpha-first-marker";
        processAndAssertAttachments(alpha);
        alpha.close();
        Map<String, String> closedSnapshot = fileSnapshot(stateRoot);
        assertFalse(closedSnapshot.isEmpty());
        assertThrows(IllegalStateException.class, alpha::process);
        assertEquals(closedSnapshot, fileSnapshot(stateRoot));

        ContractComponent beta = component("persistent-conversation",
                new RecordingModel("beta reply"));
        beta.agentKey = "beta-agent";
        beta.userPrompt = "beta-only-marker";
        processAndAssertAttachments(beta);
        assertTrue(beta.model.inputs.get(0).stream()
                .noneMatch(message -> "alpha-first-marker".equals(message.getTextContent())));
        beta.close();

        ContractComponent restored = component("persistent-conversation",
                new RecordingModel("restored reply"));
        restored.agentKey = "alpha-agent";
        restored.userPrompt = "alpha-second-marker";
        processAndAssertAttachments(restored);
        assertTrue(restored.model.inputs.get(0).stream()
                .anyMatch(message -> "alpha-first-marker".equals(message.getTextContent())));
        assertTrue(restored.model.inputs.get(0).stream()
                .anyMatch(message -> "alpha-second-marker".equals(message.getTextContent())));
        restored.close();
        Map<String, String> restoredSnapshot = fileSnapshot(stateRoot);
        assertThrows(IllegalStateException.class, restored::process);
        assertEquals(restoredSnapshot, fileSnapshot(stateRoot));
    }

    @Test
    void strictStateLoadFailureStopsBeforeModelAndLogPolicyWarnsThenContinues()
            throws Exception {
        AgentConfig config = configureAgent("state-failure-contract");
        RuntimeException loadFailure = new RuntimeException("state backend unavailable");
        ContractComponent strict = component("failed-state-conversation",
                new RecordingModel("must not run"));
        strict.agentKey = "failed-state-agent";
        strict.stateStoreResolver = ignored ->
                new ResolvedAgentStateStore(new LoadFailingStore(loadFailure), false);
        Set<String> strictAttachments = attachmentSnapshot(strict.slot);

        AgentException thrown = assertThrows(AgentException.class, strict::process);

        assertTrue(hasCause(thrown, loadFailure));
        assertEquals(0, strict.model.calls.get());
        assertEquals(strictAttachments, attachmentSnapshot(strict.slot));
        assertNoLoadFailure(strict, config);

        ContractComponent retry = component("failed-state-conversation",
                new RecordingModel("retry succeeded"));
        retry.agentKey = "failed-state-agent";
        runBounded(retry);

        config.getStateStore().setFailurePolicy(
                AgentStateStoreFailurePolicy.LOG_AND_CONTINUE);
        ContractComponent lenient = component("lenient-state-conversation",
                new RecordingModel("lenient reply"));
        lenient.agentKey = "lenient-state-agent";
        lenient.stateStoreResolver = ignored ->
                new ResolvedAgentStateStore(new LoadFailingStore(loadFailure), false);
        processAndAssertAttachments(lenient);
        assertEquals("lenient reply", lenient.slot.getResponseData());
        assertEquals(1, lenient.model.calls.get());
        assertEquals(1, lenient.warnings.size());
        assertTrue(lenient.warnings.get(0).contains(loadFailure.getMessage()));
        assertNoLoadFailure(lenient, config);
    }

    @Test
    void listenerPoliciesPreserveOrderedSourceEventsAndAlwaysReleaseInvocationState()
            throws Exception {
        AgentConfig config = configureAgent("listener-contract");
        config.getEvent().setListenerFailureMode(AgentListenerFailureMode.LOG_AND_CONTINUE);
        ContractComponent lenient = component("listener-lenient",
                new RecordingModel("listener reply"));
        lenient.agentKey = "listener-agent";
        RuntimeException listenerFailure = new RuntimeException("listener failed");
        List<FlowEvent> attempted = new CopyOnWriteArrayList<>();
        FlowEventPublisher.setListener(lenient.slot, event -> {
            attempted.add(event);
            throw listenerFailure;
        });
        Set<String> before = attachmentSnapshot(lenient.slot);

        lenient.process();

        assertEquals("listener reply", lenient.slot.getResponseData());
        assertEquals(before, attachmentSnapshot(lenient.slot));
        assertOrdered(attempted.stream().map(FlowEvent::getType).toList(),
                "agent.start", "agent.text.delta", "agent.reasoning",
                "agent.result", "agent.end");
        FlowEvent text = event(attempted, "agent.text.delta");
        FlowEvent reasoning = event(attempted, "agent.reasoning");
        assertSame(((AgentFlowEventData) text.getData()).event(),
                ((AgentFlowEventData) reasoning.getData()).event());
        assertNoLoadFailure(lenient, config);

        config.getEvent().setListenerFailureMode(AgentListenerFailureMode.FAIL_FAST);
        ContractComponent failFast = component("listener-fail-fast",
                new RecordingModel("reply after listener removal"));
        failFast.agentKey = "listener-fail-fast-agent";
        AtomicInteger attempts = new AtomicInteger();
        List<String> failFastTypes = new CopyOnWriteArrayList<>();
        FlowEventPublisher.setListener(failFast.slot, event -> {
            attempts.incrementAndGet();
            failFastTypes.add(event.getType());
            throw listenerFailure;
        });
        Set<String> failFastBefore = attachmentSnapshot(failFast.slot);

        RuntimeException thrown = assertThrows(RuntimeException.class, failFast::process);

        assertTrue(thrown == listenerFailure || hasCause(thrown, listenerFailure));
        assertEquals(1, attempts.get());
        assertFalse(failFastTypes.contains("agent.error"));
        assertEquals(failFastBefore, attachmentSnapshot(failFast.slot));
        assertNoLoadFailure(failFast, config);
        FlowEventPublisher.removeListener(failFast.slot);
        processAndAssertAttachments(failFast);
        assertEquals("reply after listener removal", failFast.slot.getResponseData());
    }

    @Test
    void errorsAndTimeoutCancelSubscriptionReleaseWorkspaceAndRestoreAttachments()
            throws Exception {
        AgentConfig config = configureAgent("terminal-cleanup-contract");
        config.getRuntime().setTimeout(Duration.ofMillis(40));
        RuntimeException modelFailure = new RuntimeException("model failed");
        ContractComponent failed = component("terminal-workspace",
                new RecordingModel("unused", new FailurePlan(modelFailure)));
        failed.agentKey = "failed-agent";
        failed.workspaceLease = true;
        Set<String> failedBefore = attachmentSnapshot(failed.slot);

        RuntimeException failedCall = assertThrows(RuntimeException.class, failed::process);

        assertTrue(failedCall == modelFailure || hasCause(failedCall, modelFailure));
        assertEquals(failedBefore, attachmentSnapshot(failed.slot));
        assertNoLoadFailure(failed, config);

        ContractComponent afterError = component("terminal-workspace",
                new RecordingModel("after error"));
        afterError.agentKey = "after-error-agent";
        afterError.workspaceLease = true;
        runBounded(afterError);

        CountDownLatch cancellation = new CountDownLatch(1);
        ContractComponent timedOut = component("terminal-workspace",
                new RecordingModel("after timeout", new NeverThenPlan(cancellation)));
        timedOut.agentKey = "timeout-agent";
        timedOut.workspaceLease = true;
        Set<String> timeoutBefore = attachmentSnapshot(timedOut.slot);

        AgentInvocationException timeout = assertThrows(
                AgentInvocationException.class, timedOut::process);

        assertEquals(AgentInvocationErrorType.TIMEOUT, timeout.getErrorType());
        assertTrue(cancellation.await(5, TimeUnit.SECONDS));
        assertTrue(timedOut.model.contexts.get(0)
                .get(LiteFlowAgentContext.class).isCancelled());
        assertEquals(timeoutBefore, attachmentSnapshot(timedOut.slot));
        assertNoLoadFailure(timedOut, config);
        runBounded(timedOut);
        assertEquals("after timeout", timedOut.slot.getResponseData());
        assertEquals(1, timedOut.runtimeBuilds.get());

        TimeoutException upstream = new TimeoutException("provider-owned timeout");
        ContractComponent upstreamTimeout = component("upstream-timeout",
                new RecordingModel("unused", new FailurePlan(upstream)));
        RuntimeException upstreamThrown = assertThrows(
                RuntimeException.class, upstreamTimeout::process);
        assertFalse(upstreamThrown instanceof AgentInvocationException);
        assertTrue(hasCause(upstreamThrown, upstream));
        assertEquals(Set.of("contract-unrelated"), attachmentSnapshot(upstreamTimeout.slot));
    }

    private AgentConfig configureAgent(String namespace) {
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace(namespace);
        agent.getRuntime().setDefaultUserId("contract-user");
        agent.getRuntime().setTimeout(Duration.ofSeconds(2));
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agent;
    }

    private AttemptGuard installGuard(AgentConfig config) throws Exception {
        AttemptGuard guard = new AttemptGuard();
        config.getInvocationGuard().setMode(AgentInvocationGuardMode.BEAN);
        config.getInvocationGuard().setBeanName("contract-guard");
        staticField(ContextAwareHolder.class, "contextAware")
                .set(null, new GuardContextAware(guard));
        return guard;
    }

    private static Field staticField(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static void append(Path path, String value) throws Exception {
        Files.writeString(path, value + System.lineSeparator(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static Map<String, String> fileSnapshot(Path root) throws Exception {
        Map<String, String> snapshot = new TreeMap<>();
        try (Stream<Path> paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                snapshot.put(root.relativize(path).toString(),
                        Base64.getEncoder().encodeToString(Files.readAllBytes(path)));
            }
        }
        return snapshot;
    }

    private static Set<String> attachmentSnapshot(Slot slot) {
        return slot instanceof TrackingSlot tracking ? tracking.attachments() : Set.of();
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) {
                return true;
            }
        }
        return false;
    }

    private static void assertOrdered(List<String> actual, String... expected) {
        int previous = -1;
        for (String value : expected) {
            int current = actual.subList(previous + 1, actual.size()).indexOf(value);
            assertTrue(current >= 0, () -> "missing ordered event " + value + " in " + actual);
            previous += current + 1;
        }
    }

    private static FlowEvent event(List<FlowEvent> events, String type) {
        return events.stream()
                .filter(event -> type.equals(event.getType()))
                .findFirst()
                .orElseThrow();
    }

    private static void runBounded(ContractComponent component) throws Exception {
        Set<String> before = attachmentSnapshot(component.slot);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> call = executor.submit(() -> {
                component.process();
                return null;
            });
            call.get(5, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(before, attachmentSnapshot(component.slot));
    }

    private static void processAndAssertAttachments(ContractComponent component)
            throws Exception {
        Set<String> before = attachmentSnapshot(component.slot);
        component.process();
        assertEquals(before, attachmentSnapshot(component.slot));
    }

    private static void assertNoLoadFailure(
            ContractComponent component, AgentConfig config) {
        AgentInvocationIdentity identity = new InvocationIdentityResolver(
                config.getRuntime().getNamespace()).resolve(
                config.getRuntime().getDefaultUserId(),
                component.slot.getConversationId(), component.agentKey());
        assertTrue(component.runtime.stateStore().takeLoadFailure(
                identity.userId(), identity.runtimeSessionId()).isEmpty());
    }

    private ContractComponent component(String conversationId, RecordingModel model) {
        Slot slot = new TrackingSlot();
        slot.setChainId("contract-chain");
        slot.setConversationId(conversationId);
        slot.putRequestId(conversationId + "-request");
        slot.setAttachment("contract-unrelated", "keep");
        ContractComponent component = new ContractComponent(slot, model);
        component.setNodeId(conversationId + "-agent");
        components.add(component);
        return component;
    }

    private static JsonNode schema() {
        return OBJECT_MAPPER.createObjectNode()
                .put("type", "object")
                .set("properties", OBJECT_MAPPER.createObjectNode()
                        .set("answer", OBJECT_MAPPER.createObjectNode().put("type", "string")));
    }

    private static String systemPrompt(List<Msg> input) {
        return input.stream()
                .filter(message -> message.getRole() == MsgRole.SYSTEM)
                .findFirst()
                .orElseThrow()
                .getTextContent();
    }

    public static final class StructuredReply {
        public String answer;
    }

    private static final class ContractComponent extends AgentComponent {
        private final Slot slot;
        private final RecordingModel model;
        private final AtomicInteger runtimeBuilds = new AtomicInteger();
        private AgentRuntime runtime;
        private String systemPrompt = "stable contract prompt";
        private String userPrompt = "contract question";
        private Class<?> outputType;
        private JsonNode outputSchema;
        private List<Object> tools = List.of();
        private String agentKey;
        private boolean workspaceLease;
        private AgentStateStoreResolver stateStoreResolver;
        private final List<String> warnings = new CopyOnWriteArrayList<>();

        private ContractComponent(Slot slot, RecordingModel model) {
            this.slot = slot;
            this.model = model;
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
        protected AgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            runtimeBuilds.incrementAndGet();
            runtime = super.buildRuntime(buildContext);
            return runtime;
        }

        @Override
        protected String systemPrompt() {
            return systemPrompt;
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return userPrompt;
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
        protected List<Object> tools() {
            return tools;
        }

        @Override
        protected String agentKey() {
            return agentKey == null ? super.agentKey() : agentKey;
        }

        @Override
        protected boolean requiresWorkspaceLease() {
            return workspaceLease;
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            return stateStoreResolver == null
                    ? super.stateStoreResolver()
                    : stateStoreResolver;
        }

        @Override
        StateStoreFailureMiddleware createStateStoreFailureMiddleware(
                GuardedNamespacedAgentStateStore stateStore,
                AgentStateStoreFailurePolicy failurePolicy) {
            return new StateStoreFailureMiddleware(
                    stateStore, failurePolicy, warnings::add);
        }
    }

    private static final class RecordingModel implements Model {
        private final String reply;
        private final boolean nativeStructuredOutput;
        private final ResponsePlan responsePlan;
        private final AtomicInteger calls = new AtomicInteger();
        private final List<RuntimeContext> contexts = new CopyOnWriteArrayList<>();
        private final List<List<Msg>> inputs = new CopyOnWriteArrayList<>();
        private final List<List<String>> toolNames = new CopyOnWriteArrayList<>();
        private final List<GenerateOptions> options = new CopyOnWriteArrayList<>();

        private RecordingModel(String reply) {
            this(reply, false);
        }

        private RecordingModel(String reply, boolean nativeStructuredOutput) {
            this(reply, nativeStructuredOutput, new ImmediatePlan());
        }

        private RecordingModel(String reply, ResponsePlan responsePlan) {
            this(reply, false, responsePlan);
        }

        private RecordingModel(
                String reply, boolean nativeStructuredOutput, ResponsePlan responsePlan) {
            this.reply = reply;
            this.nativeStructuredOutput = nativeStructuredOutput;
            this.responsePlan = responsePlan;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            calls.incrementAndGet();
            inputs.add(List.copyOf(messages));
            toolNames.add(tools.stream().map(ToolSchema::getName).sorted().toList());
            this.options.add(options);
            return Flux.deferContextual(view -> {
                contexts.add(view.get(AgentBase.RUNTIME_CONTEXT_KEY));
                ContentBlock content = TextBlock.builder().text(reply).build();
                ChatResponse response = ChatResponse.builder()
                        .content(List.of(content))
                        .finishReason("stop")
                        .build();
                return responsePlan.respond(response);
            });
        }

        @Override
        public String getModelName() {
            return "core-contract-model";
        }

        @Override
        public boolean supportsNativeStructuredOutput() {
            return nativeStructuredOutput;
        }

        @Override
        public boolean supportsNativeStructuredOutputWithTools() {
            return nativeStructuredOutput;
        }
    }

    private static final class AlphaTool {
        @Tool(name = "alpha_tool", readOnly = true)
        public String call() {
            return "alpha";
        }
    }

    private static final class BetaTool {
        @Tool(name = "beta_tool", readOnly = true)
        public String call() {
            return "beta";
        }
    }

    private interface ResponsePlan {
        Flux<ChatResponse> respond(ChatResponse response);
    }

    private static final class ImmediatePlan implements ResponsePlan {
        @Override
        public Flux<ChatResponse> respond(ChatResponse response) {
            return Flux.just(response);
        }
    }

    private static final class ActionPlan implements ResponsePlan {
        private final CheckedRunnable action;

        private ActionPlan(CheckedRunnable action) {
            this.action = action;
        }

        @Override
        public Flux<ChatResponse> respond(ChatResponse response) {
            return Flux.defer(() -> {
                try {
                    action.run();
                    return Flux.just(response);
                } catch (Exception failure) {
                    return Flux.error(failure);
                }
            });
        }
    }

    private static final class FailurePlan implements ResponsePlan {
        private final Throwable failure;

        private FailurePlan(Throwable failure) {
            this.failure = failure;
        }

        @Override
        public Flux<ChatResponse> respond(ChatResponse response) {
            return Flux.error(failure);
        }
    }

    private static final class NeverThenPlan implements ResponsePlan {
        private final CountDownLatch cancellation;
        private final AtomicInteger invocations = new AtomicInteger();

        private NeverThenPlan(CountDownLatch cancellation) {
            this.cancellation = cancellation;
        }

        @Override
        public Flux<ChatResponse> respond(ChatResponse response) {
            if (invocations.incrementAndGet() == 1) {
                return Flux.<ChatResponse>never().doOnCancel(cancellation::countDown);
            }
            return Flux.just(response);
        }
    }

    private static final class BlockingPlan implements ResponsePlan {
        private final CountDownLatch entered;
        private final CountDownLatch release;
        private final CheckedRunnable action;

        private BlockingPlan(CountDownLatch entered, CountDownLatch release) {
            this(entered, release, () -> { });
        }

        private BlockingPlan(
                CountDownLatch entered, CountDownLatch release, CheckedRunnable action) {
            this.entered = entered;
            this.release = release;
            this.action = action;
        }

        @Override
        public Flux<ChatResponse> respond(ChatResponse response) {
            return Flux.defer(() -> {
                try {
                    action.run();
                    entered.countDown();
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        return Flux.error(new AssertionError("blocking plan was not released"));
                    }
                    return Flux.just(response);
                } catch (Exception failure) {
                    return Flux.error(failure);
                }
            });
        }
    }

    private static final class ActiveBarrier implements ResponsePlan {
        private final CountDownLatch entered;
        private final CountDownLatch release = new CountDownLatch(1);
        private final AtomicInteger active = new AtomicInteger();
        private final AtomicInteger maxActive = new AtomicInteger();

        private ActiveBarrier(int parties) {
            entered = new CountDownLatch(parties);
        }

        @Override
        public Flux<ChatResponse> respond(ChatResponse response) {
            return Flux.defer(() -> {
                int current = active.incrementAndGet();
                maxActive.accumulateAndGet(current, Math::max);
                entered.countDown();
                try {
                    if (!release.await(5, TimeUnit.SECONDS)) {
                        return Flux.error(new AssertionError("active barrier was not released"));
                    }
                    return Flux.just(response);
                } catch (InterruptedException failure) {
                    Thread.currentThread().interrupt();
                    return Flux.error(failure);
                } finally {
                    active.decrementAndGet();
                }
            });
        }
    }

    @FunctionalInterface
    private interface CheckedRunnable {
        void run() throws Exception;
    }

    private static final class AttemptGuard implements AgentInvocationGuard {
        private final AgentInvocationGuard delegate = new LocalAgentInvocationGuard();
        private final List<AgentInvocationKey> stateKeys = new CopyOnWriteArrayList<>();
        private final List<AgentInvocationKey> workspaceKeys = new CopyOnWriteArrayList<>();
        private final CountDownLatch secondStateAttempt = new CountDownLatch(1);
        private final CountDownLatch secondWorkspaceAttempt = new CountDownLatch(1);

        @Override
        public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
            List<AgentInvocationKey> keys = key.scope() == AgentInvocationScope.STATE
                    ? stateKeys : workspaceKeys;
            keys.add(key);
            if (keys.size() == 2) {
                if (key.scope() == AgentInvocationScope.STATE) {
                    secondStateAttempt.countDown();
                } else {
                    secondWorkspaceAttempt.countDown();
                }
            }
            return delegate.acquire(key, timeout);
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
            return "contract-guard".equals(name) ? (T) guard : null;
        }

        @Override
        public <T> Map<String, T> getBeansOfType(Class<T> type) {
            return Map.of();
        }
    }

    private static final class TrackingSlot extends Slot {
        private final Set<String> attachmentKeys = java.util.concurrent.ConcurrentHashMap.newKeySet();

        @Override
        public <T> void setAttachment(String key, T value) {
            super.setAttachment(key, value);
            attachmentKeys.add(key);
        }

        @Override
        public void removeAttachment(String key) {
            super.removeAttachment(key);
            attachmentKeys.remove(key);
        }

        @Override
        public boolean removeAttachment(String key, Object expectedValue) {
            boolean removed = super.removeAttachment(key, expectedValue);
            if (removed) {
                attachmentKeys.remove(key);
            }
            return removed;
        }

        private Set<String> attachments() {
            return Set.copyOf(attachmentKeys);
        }
    }

    private static final class LoadFailingStore extends InMemoryAgentStateStore {
        private final RuntimeException failure;

        private LoadFailingStore(RuntimeException failure) {
            this.failure = failure;
        }

        @Override
        public <T extends io.agentscope.core.state.State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            throw failure;
        }
    }
}
