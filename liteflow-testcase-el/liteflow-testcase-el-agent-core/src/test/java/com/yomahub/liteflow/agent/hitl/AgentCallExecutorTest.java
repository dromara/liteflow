package com.yomahub.liteflow.agent.hitl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.middleware.FlowEventBridgeMiddleware;
import com.yomahub.liteflow.flow.FlowEventPublisher;
import com.yomahub.liteflow.property.agent.AgentListenerFailureMode;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.middleware.AgentInput;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionBehavior;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionRule;
import io.agentscope.core.tool.ToolBase;
import io.agentscope.core.tool.ToolCallParam;
import io.agentscope.core.tool.Toolkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import reactor.test.scheduler.VirtualTimeScheduler;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AgentCallExecutorTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Instant NOW = Instant.parse("2030-01-01T00:00:00Z");
    private static final Duration RUNTIME_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration CONFIRMATION_TIMEOUT = Duration.ofSeconds(1);

    @Test
    void realAgentScopeAskTransitionKeepsEventPendingAndMarksReplyAsking() {
        Scenario scenario = scenarioWithoutEvent();
        ToolUseBlock modelTool = new ToolUseBlock(
                "real-tool-1",
                "approval_probe",
                Map.of("query", "original"),
                "{\"query\":\"original\"}",
                Map.of("provider", "scripted"));
        Toolkit toolkit = new Toolkit();
        toolkit.registerAgentTool(new ApprovalProbeTool());
        PermissionRule askRule = new PermissionRule(
                "approval_probe", null, PermissionBehavior.ASK, "test");
        ReActAgent agent = ReActAgent.builder()
                .name("real-hitl-agent")
                .sysPrompt("Use the approval probe once.")
                .model(new ToolCallModel(modelTool))
                .toolkit(toolkit)
                .permissionContext(PermissionContextState.builder()
                        .addAskRule("approval_probe", askRule)
                        .build())
                .middlewares(List.of(new FlowEventBridgeMiddleware(
                        AgentListenerFailureMode.FAIL_FAST)))
                .build();

        try {
            Msg reply = agent.call(
                            List.of(new UserMessage("probe")), scenario.runtimeContext)
                    .block(Duration.ofSeconds(3));
            RequireUserConfirmEvent event = scenario.context.getConfirmationEvents().get(0);
            ToolUseBlock eventTool = event.getToolCalls().get(0);
            ToolUseBlock replyTool = reply.getContentBlocks(ToolUseBlock.class).get(0);

            assertEquals(ToolCallState.PENDING, eventTool.getState());
            assertEquals(ToolCallState.ASKING, replyTool.getState());
            ConfirmationRequest request = ConfirmationResultValidator.request(
                    reply, List.of(event));
            assertSame(event, request.event());
        } finally {
            agent.close();
        }
    }

    @Test
    void allowUsesSameAgentRuntimeContextOutputAndMetadataOnlyResumeMessage() {
        Scenario scenario = scenario();
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        Msg result = executor().execute(
                        scenario.agent,
                        List.of(new UserMessage("question")),
                        scenario.output,
                        scenario.runtimeContext,
                        scenario.context,
                        allowAll(),
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        assertSame(scenario.finalReply, result);
        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(2)).call(calls.capture(), same(scenario.runtimeContext));
        assertEquals("question", calls.getAllValues().get(0).get(0).getTextContent());
        assertResumeMetadataOnly(calls.getAllValues().get(1), true);
    }

    @Test
    void explicitDenyRunsContinuationAndReturnsItsReply() {
        Scenario scenario = scenario();
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));
        AgentConfirmationHandler deny = (event, context) ->
                Mono.just(List.of(new ConfirmResult(false, event.getToolCalls().get(0))));

        Msg result = executor().execute(
                        scenario.agent,
                        List.of(new UserMessage("question")),
                        scenario.output,
                        scenario.runtimeContext,
                        scenario.context,
                        deny,
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        assertSame(scenario.finalReply, result);
        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(2)).call(calls.capture(), same(scenario.runtimeContext));
        assertResumeMetadataOnly(calls.getAllValues().get(1), false);
    }

    @Test
    void normalContinuationHandlesASecondAskBeforeReturningFinalReply() {
        Scenario scenario = scenario();
        ToolUseBlock secondTool = tool("tool-2", "search");
        RequireUserConfirmEvent secondEvent = new RequireUserConfirmEvent(
                "reply-2", List.of(secondTool));
        Msg secondReply = askingReply("reply-2", secondTool);
        AtomicInteger handlerCalls = new AtomicInteger();
        AgentConfirmationHandler handler = (event, context) -> {
            assertSame(scenario.context, context);
            handlerCalls.incrementAndGet();
            return Mono.just(event.getToolCalls().stream()
                    .map(tool -> new ConfirmResult(true, tool))
                    .toList());
        };
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(
                        initialAsking(scenario),
                        askingRound(scenario.context, secondEvent, secondReply),
                        Mono.just(scenario.finalReply));

        Msg result = executor().execute(
                        scenario.agent,
                        List.of(new UserMessage("question")),
                        scenario.output,
                        scenario.runtimeContext,
                        scenario.context,
                        handler,
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        assertSame(scenario.finalReply, result);
        assertEquals(2, handlerCalls.get());
        verify(scenario.agent, times(3)).call(anyList(), same(scenario.runtimeContext));
        assertEquals(2, scenario.context.getConfirmationEvents().size());
    }

    @Test
    void cleanupKeepsDenyingAdditionalAskRoundsWithoutInvokingHandler() {
        Scenario scenario = scenario();
        AtomicInteger handlerCalls = new AtomicInteger();
        ToolUseBlock secondTool = tool("tool-2", "search");
        RequireUserConfirmEvent secondEvent = new RequireUserConfirmEvent(
                "reply-2", List.of(secondTool));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(
                        initialAsking(scenario),
                        askingRound(
                                scenario.context,
                                secondEvent,
                                askingReply("reply-2", secondTool)),
                        Mono.just(scenario.finalReply));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                (event, context) -> {
                                    handlerCalls.incrementAndGet();
                                    return Mono.error(new IllegalStateException(
                                            "start cleanup"));
                                },
                                CONFIRMATION_TIMEOUT,
                                false,
                                RUNTIME_TIMEOUT)
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        assertEquals(1, handlerCalls.get());
        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(3)).call(calls.capture(), same(scenario.runtimeContext));
        assertResumeMetadataOnly(calls.getAllValues().get(1), false);
        assertResumeMetadataOnly(calls.getAllValues().get(2), false);
    }

    @Test
    void repeatedCleanupAskingUsesOneAbsoluteDeadlineAndKeepsIntendedFailure() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Supplier<Instant> now = () -> NOW.plusMillis(scheduler.now(TimeUnit.MILLISECONDS));
        Scenario scenario = scenario(NOW.plusSeconds(20));
        ToolUseBlock repeatedTool = tool("cleanup-tool", "search");
        RequireUserConfirmEvent repeatedEvent = new RequireUserConfirmEvent(
                "cleanup-reply", List.of(repeatedTool));
        Msg repeatedReply = askingReply("cleanup-reply", repeatedTool);
        Mono<Msg> slowRepeatedAsk = Mono.delay(Duration.ofSeconds(2), scheduler)
                .then(askingRound(scenario.context, repeatedEvent, repeatedReply));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), slowRepeatedAsk);

        Mono<Msg> invocation = new AgentCallExecutor(now, scheduler).execute(
                scenario.agent,
                List.of(new UserMessage("question")),
                scenario.output,
                scenario.runtimeContext,
                scenario.context,
                null,
                CONFIRMATION_TIMEOUT,
                false,
                Duration.ofSeconds(3));

        StepVerifier.withVirtualTime(() -> invocation, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(3))
                .expectErrorSatisfies(failure -> {
                    AgentInvocationException typed = assertInstanceOf(
                            AgentInvocationException.class, failure);
                    assertEquals(AgentInvocationErrorType.PERMISSION, typed.getErrorType());
                    assertTrue(List.of(typed.getSuppressed()).stream()
                            .anyMatch(suppressed -> suppressed instanceof TimeoutException
                                    && suppressed.getMessage().contains(
                                            "Denied HITL cleanup exceeded timeout")));
                })
                .verify();
        verify(scenario.agent, times(3)).call(anyList(), same(scenario.runtimeContext));
    }

    @Test
    void synchronousImmediateCleanupRoundsAreStackSafe() {
        Scenario scenario = scenario();
        AtomicInteger calls = new AtomicInteger();
        int askingRounds = 2_000;
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenAnswer(invocation -> Mono.fromSupplier(() -> {
                    int call = calls.incrementAndGet();
                    if (call <= askingRounds) {
                        scenario.context.recordConfirmationEvent(scenario.event);
                        return scenario.askingReply;
                    }
                    return scenario.finalReply;
                }));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                (event, context) -> Mono.error(
                                        new IllegalStateException("start cleanup")),
                                CONFIRMATION_TIMEOUT,
                                false,
                                Duration.ofSeconds(5))
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        assertEquals(askingRounds + 1, calls.get());
    }

    @Test
    void handlerIsInvokedOnlyAfterFirstCallMonoTerminates() {
        Scenario scenario = scenario();
        AtomicBoolean firstCallTerminated = new AtomicBoolean();
        AtomicBoolean handlerObservedTermination = new AtomicBoolean();
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario)
                                .doOnTerminate(() -> firstCallTerminated.set(true)),
                        Mono.just(scenario.finalReply));
        AgentConfirmationHandler handler = (event, context) -> {
            handlerObservedTermination.set(firstCallTerminated.get());
            return Mono.just(List.of(new ConfirmResult(true, event.getToolCalls().get(0))));
        };

        Msg result = executor().execute(
                        scenario.agent,
                        List.of(new UserMessage("question")),
                        scenario.output,
                        scenario.runtimeContext,
                        scenario.context,
                        handler,
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        assertSame(scenario.finalReply, result);
        assertTrue(handlerObservedTermination.get());
    }

    @Test
    void missingNullEmptyAndFailingHandlersCleanupAsDeniedBeforePermissionFailure() {
        assertHandlerFailure(null, null);
        assertHandlerFailure((event, context) -> null, NullPointerException.class);
        assertHandlerFailure((event, context) -> Mono.empty(), null);
        assertHandlerFailure(
                (event, context) -> Mono.error(new IllegalStateException("handler failed")),
                IllegalStateException.class);
    }

    @ParameterizedTest
    @EnumSource(
            value = AgentInvocationErrorType.class,
            names = {"STRUCTURED_OUTPUT", "TIMEOUT", "INTERRUPTED"})
    void typedHandlerErrorsArePermissionFailuresWithOriginalCauseAfterCleanup(
            AgentInvocationErrorType handlerType) {
        Scenario scenario = scenario();
        AgentInvocationException handlerFailure = new AgentInvocationException(
                handlerType, "handler-owned " + handlerType);
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                (event, context) -> Mono.error(handlerFailure),
                                CONFIRMATION_TIMEOUT,
                                false,
                                RUNTIME_TIMEOUT)
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        assertSame(handlerFailure, thrown.getCause());
        assertFalse(scenario.context.isCancelled());
        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(2)).call(calls.capture(), same(scenario.runtimeContext));
        assertResumeMetadataOnly(calls.getAllValues().get(1), false);
    }

    @Test
    void replyEventAndToolProtocolFailuresCleanupAsDeniedBeforePermissionFailure() {
        Scenario missingEvent = scenarioWithoutEvent();
        assertProtocolFailure(missingEvent, allowAll());

        Scenario replyMismatch = scenarioWithEvent(
                new RequireUserConfirmEvent("different-reply", List.of(tool("tool-1", "search"))));
        assertProtocolFailure(replyMismatch, allowAll());

        Scenario toolMismatch = scenarioWithEvent(new RequireUserConfirmEvent(
                "reply-1", List.of(tool("tool-1", "different-name"))));
        assertProtocolFailure(toolMismatch, allowAll());

        Scenario duplicateEvent = scenario();
        duplicateEvent = duplicateEvent.withEventCopies(2);
        assertProtocolFailure(duplicateEvent, allowAll());
    }

    @Test
    void eventAndReplyToolCorrelationRejectsEveryNonStateFieldMismatch() {
        assertProtocolFailure(scenarioWithEvent(new RequireUserConfirmEvent(
                "reply-1",
                List.of(new ToolUseBlock(
                        "tool-1", "search", Map.of("query", "changed"), null, Map.of(),
                        ToolCallState.ASKING)))), allowAll());
        assertProtocolFailure(scenarioWithEvent(new RequireUserConfirmEvent(
                "reply-1",
                List.of(new ToolUseBlock(
                        "tool-1", "search", Map.of("query", "original"), "raw", Map.of(),
                        ToolCallState.ASKING)))), allowAll());
        assertProtocolFailure(scenarioWithEvent(new RequireUserConfirmEvent(
                "reply-1",
                List.of(new ToolUseBlock(
                        "tool-1", "search", Map.of("query", "original"), null,
                        Map.of("provider", "changed"), ToolCallState.ASKING)))), allowAll());
        assertProtocolFailure(scenarioWithEvent(new RequireUserConfirmEvent(
                "reply-1",
                List.of(new ToolUseBlock(
                        "tool-1", "search", Map.of("query", "original"), null, Map.of(),
                        ToolCallState.ALLOWED)))), allowAll());
    }

    @Test
    void handlerResultsMustCoverEveryPendingToolExactlyOnceAndRetainIdAndName() {
        Scenario missing = scenario();
        assertProtocolFailure(missing, (event, context) -> Mono.just(List.of()));

        Scenario duplicate = scenario();
        assertProtocolFailure(duplicate, (event, context) -> Mono.just(List.of(
                new ConfirmResult(true, duplicate.tool),
                new ConfirmResult(false, duplicate.tool))));

        Scenario unknown = scenario();
        assertProtocolFailure(unknown, (event, context) -> Mono.just(List.of(
                new ConfirmResult(true, tool("unknown", "search")))));

        Scenario renamed = scenario();
        assertProtocolFailure(renamed, (event, context) -> Mono.just(List.of(
                new ConfirmResult(true, tool("tool-1", "renamed")))));
    }

    @Test
    void handlerTimeoutCleansAsDeniedBeforeExposingTypedTimeout() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Supplier<Instant> now = () -> NOW.plusMillis(scheduler.now(TimeUnit.MILLISECONDS));
        Scenario scenario = scenario(NOW.plusSeconds(10));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        Mono<Msg> invocation = new AgentCallExecutor(now, scheduler).execute(
                scenario.agent,
                List.of(new UserMessage("question")),
                scenario.output,
                scenario.runtimeContext,
                scenario.context,
                (event, context) -> Mono.never(),
                Duration.ofSeconds(2),
                false,
                Duration.ofSeconds(10));

        StepVerifier.withVirtualTime(() -> invocation, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(2))
                .expectErrorSatisfies(failure -> {
                    AgentInvocationException typed = assertInstanceOf(
                            AgentInvocationException.class, failure);
                    assertEquals(AgentInvocationErrorType.TIMEOUT, typed.getErrorType());
                    assertInstanceOf(TimeoutException.class, typed.getCause());
                })
                .verify();
        verify(scenario.agent, times(2)).call(anyList(), same(scenario.runtimeContext));
        assertTrue(scenario.context.isCancelled());
    }

    @Test
    void equalRuntimeAndConfirmationDefaultsStillRunCleanupAfterLogicalDeadline() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Supplier<Instant> now = () -> NOW.plusMillis(scheduler.now(TimeUnit.MILLISECONDS));
        Duration equalDefaults = Duration.ofMinutes(2);
        Scenario scenario = scenario(NOW.plus(equalDefaults));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(Mono.delay(Duration.ofMinutes(1), scheduler)
                                .then(initialAsking(scenario)),
                        Mono.just(scenario.finalReply));

        Mono<Msg> invocation = new AgentCallExecutor(now, scheduler).execute(
                scenario.agent,
                List.of(new UserMessage("question")),
                scenario.output,
                scenario.runtimeContext,
                scenario.context,
                (event, context) -> Mono.never(),
                equalDefaults,
                false,
                equalDefaults);

        StepVerifier.withVirtualTime(() -> invocation, () -> scheduler, 1)
                .thenAwait(equalDefaults)
                .expectErrorSatisfies(failure -> {
                    AgentInvocationException typed = assertInstanceOf(
                            AgentInvocationException.class, failure);
                    assertEquals(AgentInvocationErrorType.TIMEOUT, typed.getErrorType());
                    assertTrue(typed.getMessage().contains("runtime timeout"));
                })
                .verify();
        verify(scenario.agent, times(2)).call(anyList(), same(scenario.runtimeContext));
    }

    @Test
    void cleanupFailureIsSuppressedInsteadOfMaskingTypedPermissionFailure() {
        Scenario scenario = scenario();
        IllegalStateException cleanupFailure = new IllegalStateException("cleanup failed");
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.error(cleanupFailure));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                null,
                                CONFIRMATION_TIMEOUT,
                                false,
                                RUNTIME_TIMEOUT)
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        assertTrue(List.of(thrown.getSuppressed()).contains(cleanupFailure));
    }

    @Test
    void failOnDeniedToolUsesAllDeniedCleanupThenRaisesPermissionFailure() {
        Scenario scenario = scenario();
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                (event, context) -> Mono.just(List.of(
                                        new ConfirmResult(false, scenario.tool))),
                                CONFIRMATION_TIMEOUT,
                                true,
                                RUNTIME_TIMEOUT)
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(2)).call(calls.capture(), same(scenario.runtimeContext));
        assertResumeMetadataOnly(calls.getAllValues().get(1), false);
    }

    @Test
    void modifiedInputIsAcceptedOnlyWhenToolIdAndNameAreRetained() {
        Scenario scenario = scenario();
        ToolUseBlock modified = new ToolUseBlock(
                "tool-1", "search", Map.of("query", "approved"));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        executor().execute(
                        scenario.agent,
                        List.of(new UserMessage("question")),
                        scenario.output,
                        scenario.runtimeContext,
                        scenario.context,
                        (event, context) -> Mono.just(List.of(new ConfirmResult(true, modified))),
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(2)).call(calls.capture(), same(scenario.runtimeContext));
        ConfirmResult resumed = confirmationResults(calls.getAllValues().get(1)).get(0);
        assertEquals(Map.of("query", "approved"), resumed.getToolCall().getInput());
    }

    @Test
    void textJavaTypeAndJsonSchemaReuseTheSameOutputOverloadForBothCalls() {
        assertOutputMode(AgentOutputSpec.text());
        assertOutputMode(AgentOutputSpec.javaType(StructuredReply.class));
        assertOutputMode(AgentOutputSpec.jsonSchema(
                MAPPER.createObjectNode().put("type", "object")));
    }

    @Test
    void providerNeutralCallTargetSupportsEveryRuntimeContextOutputShape() {
        assertProviderNeutralOutputMode(AgentOutputSpec.text());
        assertProviderNeutralOutputMode(AgentOutputSpec.javaType(StructuredReply.class));
        assertProviderNeutralOutputMode(AgentOutputSpec.jsonSchema(
                MAPPER.createObjectNode().put("type", "object")));
    }

    @Test
    void upstreamTimeoutExceptionIsNeverMisclassifiedAsFrameworkDeadline() {
        Scenario scenario = scenario();
        TimeoutException upstream = new TimeoutException("upstream-owned timeout");
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(Mono.error(upstream));

        RuntimeException thrown = assertThrows(
                RuntimeException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                allowAll(),
                                CONFIRMATION_TIMEOUT,
                                false,
                                RUNTIME_TIMEOUT)
                        .block());

        assertFalse(thrown instanceof AgentInvocationException);
        assertSame(upstream, thrown.getCause());
    }

    @Test
    void initialCallRuntimeTimeoutUsesFrameworkMarkerWithoutCleanup() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Supplier<Instant> now = () -> NOW.plusMillis(scheduler.now(TimeUnit.MILLISECONDS));
        Scenario scenario = scenario(NOW.plusSeconds(2));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(Mono.never());

        Mono<Msg> invocation = new AgentCallExecutor(now, scheduler).execute(
                scenario.agent,
                List.of(new UserMessage("question")),
                scenario.output,
                scenario.runtimeContext,
                scenario.context,
                allowAll(),
                Duration.ofSeconds(10),
                false,
                Duration.ofSeconds(3));

        StepVerifier.withVirtualTime(() -> invocation, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(2))
                .expectErrorSatisfies(failure -> {
                    AgentInvocationException typed = assertInstanceOf(
                            AgentInvocationException.class, failure);
                    assertEquals(AgentInvocationErrorType.TIMEOUT, typed.getErrorType());
                    assertTrue(typed.getMessage().contains("runtime timeout"));
                    assertInstanceOf(TimeoutException.class, typed.getCause());
                })
                .verify();
        assertTrue(scenario.context.isCancelled());
        verify(scenario.agent).call(anyList(), same(scenario.runtimeContext));
    }

    @Test
    void continuationRuntimeTimeoutUsesPrivateMarkerAndDoesNotClearCancellation() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Supplier<Instant> now = () -> NOW.plusMillis(scheduler.now(TimeUnit.MILLISECONDS));
        Scenario scenario = scenario(NOW.plusSeconds(2));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.never());
        Mono<Msg> invocation = new AgentCallExecutor(now, scheduler).execute(
                scenario.agent,
                List.of(new UserMessage("question")),
                scenario.output,
                scenario.runtimeContext,
                scenario.context,
                allowAll(),
                Duration.ofSeconds(10),
                false,
                Duration.ofSeconds(3));

        StepVerifier.withVirtualTime(() -> invocation, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(2))
                .thenAwait(Duration.ofSeconds(3))
                .expectErrorSatisfies(failure -> {
                    AgentInvocationException typed = assertInstanceOf(
                            AgentInvocationException.class, failure);
                    assertEquals(AgentInvocationErrorType.TIMEOUT, typed.getErrorType());
                    assertTrue(typed.getMessage().contains("runtime timeout"));
                    assertInstanceOf(TimeoutException.class, typed.getCause());
                })
                .verify();
        assertTrue(scenario.context.isCancelled());
        verify(scenario.agent, times(3)).call(anyList(), same(scenario.runtimeContext));
    }

    @Test
    void multipleNormalRoundsShareOneLogicalRuntimeDeadline() {
        VirtualTimeScheduler scheduler = VirtualTimeScheduler.create();
        Supplier<Instant> now = () -> NOW.plusMillis(scheduler.now(TimeUnit.MILLISECONDS));
        Scenario scenario = scenario(NOW.plusSeconds(3));
        ToolUseBlock secondTool = tool("tool-2", "search");
        RequireUserConfirmEvent secondEvent = new RequireUserConfirmEvent(
                "reply-2", List.of(secondTool));
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(
                        initialAsking(scenario),
                        Mono.delay(Duration.ofSeconds(2), scheduler).then(askingRound(
                                scenario.context,
                                secondEvent,
                                askingReply("reply-2", secondTool))),
                        Mono.never(),
                        Mono.just(scenario.finalReply));

        Mono<Msg> invocation = new AgentCallExecutor(now, scheduler).execute(
                scenario.agent,
                List.of(new UserMessage("question")),
                scenario.output,
                scenario.runtimeContext,
                scenario.context,
                allowAll(),
                Duration.ofSeconds(10),
                false,
                Duration.ofSeconds(2));

        StepVerifier.withVirtualTime(() -> invocation, () -> scheduler, 1)
                .thenAwait(Duration.ofSeconds(3))
                .expectErrorSatisfies(failure -> {
                    AgentInvocationException typed = assertInstanceOf(
                            AgentInvocationException.class, failure);
                    assertEquals(AgentInvocationErrorType.TIMEOUT, typed.getErrorType());
                    assertTrue(typed.getMessage().contains("runtime timeout"));
                })
                .verify();
        assertTrue(scenario.context.isCancelled());
        verify(scenario.agent, times(4)).call(anyList(), same(scenario.runtimeContext));
    }

    @Test
    void flowBridgeRecordsConfirmationBeforeListenerAndAlsoWithoutListener() {
        Scenario scenario = scenarioWithoutEvent();
        RequireUserConfirmEvent event = new RequireUserConfirmEvent(
                "reply-1", List.of(scenario.tool));
        AtomicInteger recordedAtDelivery = new AtomicInteger(-1);
        FlowEventPublisher.setListener(scenario.context.getSlot(),
                ignored -> recordedAtDelivery.set(
                        scenario.context.getConfirmationEvents().size()));
        FlowEventBridgeMiddleware middleware = new FlowEventBridgeMiddleware(
                AgentListenerFailureMode.FAIL_FAST);

        middleware.onAgent(
                        null,
                        scenario.runtimeContext,
                        new AgentInput(List.of()),
                        ignored -> Flux.just(event))
                .blockLast();

        assertEquals(1, recordedAtDelivery.get());
        assertSame(event, scenario.context.getConfirmationEvents().get(0));

        FlowEventPublisher.setListener(scenario.context.getSlot(), null);
        Scenario noListener = scenarioWithoutEvent();
        middleware.onAgent(
                        null,
                        noListener.runtimeContext,
                        new AgentInput(List.of()),
                        ignored -> Flux.just(event))
                .blockLast();
        assertSame(event, noListener.context.getConfirmationEvents().get(0));
    }

    @Test
    void resolverPrefersExplicitTreatsNullAsZeroAndRejectsMultipleOrLookupFailure() {
        AgentConfirmationHandler explicit = allowAll();
        AtomicInteger lookups = new AtomicInteger();
        AgentConfirmationHandlerResolver explicitResolver =
                new AgentConfirmationHandlerResolver(() -> {
                    lookups.incrementAndGet();
                    return Map.of("container", allowAll());
                });
        assertSame(explicit, explicitResolver.resolve(explicit));
        assertEquals(0, lookups.get());
        assertEquals(null, new AgentConfirmationHandlerResolver(() -> null).resolve(null));
        assertEquals(null, new AgentConfirmationHandlerResolver(Map::of).resolve(null));

        Map<String, AgentConfirmationHandler> multiple = new LinkedHashMap<>();
        multiple.put("first", allowAll());
        multiple.put("second", allowAll());
        assertThrows(com.yomahub.liteflow.agent.exception.AgentConfigException.class,
                () -> new AgentConfirmationHandlerResolver(() -> multiple).resolve(null));

        IllegalStateException lookupFailure = new IllegalStateException("container failed");
        com.yomahub.liteflow.agent.exception.AgentConfigException wrapped = assertThrows(
                com.yomahub.liteflow.agent.exception.AgentConfigException.class,
                () -> new AgentConfirmationHandlerResolver(() -> {
                    throw lookupFailure;
                }).resolve(null));
        assertSame(lookupFailure, wrapped.getCause());
    }

    private static void assertOutputMode(AgentOutputSpec output) {
        Scenario scenario = scenario(output, NOW.plus(RUNTIME_TIMEOUT));
        switch (output.kind()) {
            case TEXT -> when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                    .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));
            case JAVA_TYPE -> when(scenario.agent.call(
                            anyList(), any(Class.class), same(scenario.runtimeContext)))
                    .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));
            case JSON_SCHEMA -> when(scenario.agent.call(
                            anyList(), any(com.fasterxml.jackson.databind.JsonNode.class),
                            same(scenario.runtimeContext)))
                    .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));
        }

        Msg result = executor().execute(
                        scenario.agent,
                        List.of(new UserMessage("question")),
                        output,
                        scenario.runtimeContext,
                        scenario.context,
                        allowAll(),
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        assertSame(scenario.finalReply, result);
        switch (output.kind()) {
            case TEXT -> verify(scenario.agent, times(2))
                    .call(anyList(), same(scenario.runtimeContext));
            case JAVA_TYPE -> verify(scenario.agent, times(2))
                    .call(anyList(), same(output.javaType()), same(scenario.runtimeContext));
            case JSON_SCHEMA -> {
                ArgumentCaptor<com.fasterxml.jackson.databind.JsonNode> schemas =
                        ArgumentCaptor.forClass(com.fasterxml.jackson.databind.JsonNode.class);
                verify(scenario.agent, times(2))
                        .call(anyList(), schemas.capture(), same(scenario.runtimeContext));
                assertEquals(output.jsonSchema(), schemas.getAllValues().get(0));
                assertSame(schemas.getAllValues().get(0), schemas.getAllValues().get(1));
            }
        }
    }

    private static void assertProviderNeutralOutputMode(AgentOutputSpec output) {
        Scenario scenario = scenario(output, NOW.plus(RUNTIME_TIMEOUT));
        AtomicInteger textCalls = new AtomicInteger();
        AtomicInteger javaTypeCalls = new AtomicInteger();
        AtomicInteger schemaCalls = new AtomicInteger();
        AgentCallTarget target = new AgentCallTarget() {
            @Override
            public Mono<Msg> call(List<Msg> input, RuntimeContext runtimeContext) {
                assertSame(scenario.runtimeContext, runtimeContext);
                textCalls.incrementAndGet();
                return Mono.just(scenario.finalReply);
            }

            @Override
            public Mono<Msg> call(
                    List<Msg> input, Class<?> javaType, RuntimeContext runtimeContext) {
                assertSame(scenario.runtimeContext, runtimeContext);
                assertSame(output.javaType(), javaType);
                javaTypeCalls.incrementAndGet();
                return Mono.just(scenario.finalReply);
            }

            @Override
            public Mono<Msg> call(
                    List<Msg> input,
                    com.fasterxml.jackson.databind.JsonNode jsonSchema,
                    RuntimeContext runtimeContext) {
                assertSame(scenario.runtimeContext, runtimeContext);
                assertEquals(output.jsonSchema(), jsonSchema);
                schemaCalls.incrementAndGet();
                return Mono.just(scenario.finalReply);
            }
        };

        Msg result = executor().execute(
                        target,
                        List.of(new UserMessage("question")),
                        output,
                        scenario.runtimeContext,
                        scenario.context,
                        allowAll(),
                        CONFIRMATION_TIMEOUT,
                        false,
                        RUNTIME_TIMEOUT)
                .block();

        assertSame(scenario.finalReply, result);
        assertEquals(output.kind() == AgentOutputSpec.Kind.TEXT ? 1 : 0, textCalls.get());
        assertEquals(output.kind() == AgentOutputSpec.Kind.JAVA_TYPE ? 1 : 0, javaTypeCalls.get());
        assertEquals(output.kind() == AgentOutputSpec.Kind.JSON_SCHEMA ? 1 : 0, schemaCalls.get());
    }

    private static void assertHandlerFailure(
            AgentConfirmationHandler handler, Class<? extends Throwable> expectedCause) {
        Scenario scenario = scenario();
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                handler,
                                CONFIRMATION_TIMEOUT,
                                false,
                                RUNTIME_TIMEOUT)
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        if (expectedCause != null) {
            assertInstanceOf(expectedCause, thrown.getCause());
        }
        ArgumentCaptor<List<Msg>> calls = listCaptor();
        verify(scenario.agent, times(2)).call(calls.capture(), same(scenario.runtimeContext));
        assertResumeMetadataOnly(calls.getAllValues().get(1), false);
    }

    private static void assertProtocolFailure(
            Scenario scenario, AgentConfirmationHandler handler) {
        when(scenario.agent.call(anyList(), same(scenario.runtimeContext)))
                .thenReturn(initialAsking(scenario), Mono.just(scenario.finalReply));

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class,
                () -> executor().execute(
                                scenario.agent,
                                List.of(new UserMessage("question")),
                                scenario.output,
                                scenario.runtimeContext,
                                scenario.context,
                                handler,
                                CONFIRMATION_TIMEOUT,
                                false,
                                RUNTIME_TIMEOUT)
                        .block());

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        verify(scenario.agent, times(2)).call(anyList(), same(scenario.runtimeContext));
    }

    private static AgentCallExecutor executor() {
        return new AgentCallExecutor(
                () -> NOW, reactor.core.scheduler.Schedulers.parallel());
    }

    private static AgentConfirmationHandler allowAll() {
        return (event, context) -> Mono.just(event.getToolCalls().stream()
                .map(tool -> new ConfirmResult(true, tool))
                .toList());
    }

    private static Scenario scenario() {
        return scenario(NOW.plus(RUNTIME_TIMEOUT));
    }

    private static Scenario scenarioWithoutEvent() {
        return scenario(AgentOutputSpec.text(), NOW.plus(RUNTIME_TIMEOUT), null);
    }

    private static Scenario scenarioWithEvent(RequireUserConfirmEvent event) {
        return scenario(AgentOutputSpec.text(), NOW.plus(RUNTIME_TIMEOUT), event);
    }

    private static Scenario scenario(Instant deadline) {
        return scenario(AgentOutputSpec.text(), deadline);
    }

    private static Scenario scenario(AgentOutputSpec output, Instant deadline) {
        return scenario(output, deadline, null, true);
    }

    private static Scenario scenario(
            AgentOutputSpec output, Instant deadline, RequireUserConfirmEvent event) {
        return scenario(output, deadline, event, false);
    }

    private static Scenario scenario(
            AgentOutputSpec output,
            Instant deadline,
            RequireUserConfirmEvent suppliedEvent,
            boolean addDefaultEvent) {
        ToolUseBlock tool = tool("tool-1", "search");
        String replyId = "reply-1";
        Msg askingReply = AssistantMessage.builder()
                .content(tool)
                .metadata(Map.of(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID, replyId))
                .generateReason(GenerateReason.PERMISSION_ASKING)
                .build();
        Slot slot = new Slot();
        slot.setChainId("chain-1");
        slot.setConversationId("conversation-1");
        slot.putRequestId("request-1");
        LiteFlowAgentContext context = new LiteFlowAgentContext(
                new AgentInvocationIdentity(
                        "namespace-1",
                        "user-1",
                        "conversation-1",
                        "agent-1",
                        null,
                        null,
                        null),
                slot,
                "chain-1",
                "node-1",
                "request-1",
                "trace-1",
                deadline,
                output,
                LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX + "hitl-test");
        RequireUserConfirmEvent event = suppliedEvent;
        if (addDefaultEvent) {
            event = new RequireUserConfirmEvent(replyId, List.of(tool));
        }
        RuntimeContext runtimeContext = RuntimeContext.builder()
                .userId(context.getRuntimeUserId())
                .sessionId(context.getRuntimeSessionId())
                .put(LiteFlowAgentContext.class, context)
                .put(Slot.class, slot)
                .build();
        return new Scenario(
                mock(ReActAgent.class),
                output,
                context,
                runtimeContext,
                tool,
                event,
                1,
                askingReply,
                AssistantMessage.builder().textContent("final reply").build());
    }

    private static Mono<Msg> initialAsking(Scenario scenario) {
        return Mono.fromSupplier(() -> {
            for (int i = 0; i < scenario.eventCopies; i++) {
                if (scenario.event != null) {
                    scenario.context.recordConfirmationEvent(scenario.event);
                }
            }
            return scenario.askingReply;
        });
    }

    private static Mono<Msg> askingRound(
            LiteFlowAgentContext context,
            RequireUserConfirmEvent event,
            Msg reply) {
        return Mono.fromSupplier(() -> {
            context.recordConfirmationEvent(event);
            return reply;
        });
    }

    private static Msg askingReply(String replyId, ToolUseBlock tool) {
        return AssistantMessage.builder()
                .content(tool)
                .metadata(Map.of(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID, replyId))
                .generateReason(GenerateReason.PERMISSION_ASKING)
                .build();
    }

    private static ToolUseBlock tool(String id, String name) {
        return new ToolUseBlock(
                id,
                name,
                Map.of("query", "original"),
                null,
                Map.of(),
                ToolCallState.ASKING);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static ArgumentCaptor<List<Msg>> listCaptor() {
        return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
    }

    @SuppressWarnings("unchecked")
    private static void assertResumeMetadataOnly(List<Msg> messages, boolean confirmed) {
        assertEquals(1, messages.size());
        Msg resume = messages.get(0);
        assertInstanceOf(UserMessage.class, resume);
        assertTrue(resume.getContent().isEmpty());
        assertEquals(1, resume.getMetadata().size());
        assertTrue(resume.getMetadata().containsKey(Msg.METADATA_CONFIRM_RESULTS));
        assertFalse(resume.getMetadata().containsKey(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID));
        List<ConfirmResult> results = confirmationResults(messages);
        assertEquals(1, results.size());
        assertEquals(confirmed, results.get(0).isConfirmed());
    }

    @SuppressWarnings("unchecked")
    private static List<ConfirmResult> confirmationResults(List<Msg> messages) {
        return (List<ConfirmResult>) messages.get(0).getMetadata()
                .get(Msg.METADATA_CONFIRM_RESULTS);
    }

    private record StructuredReply(String value) {
    }

    private static final class ToolCallModel implements Model {

        private final ToolUseBlock tool;

        private ToolCallModel(ToolUseBlock tool) {
            this.tool = tool;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            return Flux.just(ChatResponse.builder()
                    .content(List.<ContentBlock>of(tool))
                    .finishReason("tool_calls")
                    .build());
        }

        @Override
        public String getModelName() {
            return "hitl-tool-call-model";
        }
    }

    private static final class ApprovalProbeTool extends ToolBase {

        private ApprovalProbeTool() {
            super(ToolBase.builder()
                    .name("approval_probe")
                    .description("Probe permission handling")
                    .inputSchema(Map.of(
                            "type", "object",
                            "properties", Map.of("query", Map.of("type", "string")))));
        }

        @Override
        public Mono<io.agentscope.core.message.ToolResultBlock> callAsync(ToolCallParam param) {
            return Mono.error(new AssertionError("ASK tool must not execute before confirmation"));
        }
    }

    private record Scenario(
            ReActAgent agent,
            AgentOutputSpec output,
            LiteFlowAgentContext context,
            RuntimeContext runtimeContext,
            ToolUseBlock tool,
            RequireUserConfirmEvent event,
            int eventCopies,
            Msg askingReply,
            Msg finalReply) {

        private Scenario withEventCopies(int copies) {
            return new Scenario(
                    agent,
                    output,
                    context,
                    runtimeContext,
                    tool,
                    event,
                    copies,
                    askingReply,
                    finalReply);
        }
    }
}
