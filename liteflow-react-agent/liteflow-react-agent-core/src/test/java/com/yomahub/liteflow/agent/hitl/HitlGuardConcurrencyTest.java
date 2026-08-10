package com.yomahub.liteflow.agent.hitl;

import com.yomahub.liteflow.agent.component.AbstractAgentComponent;
import com.yomahub.liteflow.agent.component.ReActAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.event.ConfirmResult;
import io.agentscope.core.event.RequireUserConfirmEvent;
import io.agentscope.core.message.AssistantMessage;
import io.agentscope.core.message.GenerateReason;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.ToolCallState;
import io.agentscope.core.message.ToolUseBlock;
import io.agentscope.core.state.InMemoryAgentStateStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HitlGuardConcurrencyTest {

    private final List<TestComponent> components = new ArrayList<>();

    @AfterEach
    void cleanConfigurationAndComponents() {
        components.forEach(TestComponent::close);
        LiteflowConfigGetter.clean();
    }

    @Test
    void outerLeaseCoversFirstCallHandlerWaitAndContinuation() throws Exception {
        configure(Duration.ofSeconds(5), Duration.ofSeconds(2));
        ToolUseBlock pending = tool();
        CountDownLatch handlerEntered = new CountDownLatch(1);
        CountDownLatch continuationEntered = new CountDownLatch(1);
        CountDownLatch secondEntered = new CountDownLatch(1);
        Sinks.One<List<ConfirmResult>> handlerRelease = Sinks.one();
        Sinks.One<Msg> continuationRelease = Sinks.one();
        TestComponent first = component(slot(), (event, context) -> {
            handlerEntered.countDown();
            return handlerRelease.asMono();
        });
        TestComponent second = component(slot(), null);
        first.answer = (messages, runtimeContext) -> {
            if (!messages.get(0).getContent().isEmpty()) {
                runtimeContext.get(LiteFlowAgentContext.class).recordConfirmationEvent(
                        new RequireUserConfirmEvent("reply-1", List.of(pending)));
                return Mono.just(askingReply(pending));
            }
            continuationEntered.countDown();
            return continuationRelease.asMono();
        };
        second.answer = (messages, runtimeContext) -> {
            secondEntered.countDown();
            return Mono.just(AssistantMessage.builder().textContent("second").build());
        };
        ExecutorService pool = Executors.newFixedThreadPool(2);

        try {
            Future<?> firstRun = pool.submit(() -> {
                first.process();
                return null;
            });
            assertTrue(handlerEntered.await(5, TimeUnit.SECONDS));
            Future<?> secondRun = pool.submit(() -> {
                second.process();
                return null;
            });
            assertFalse(secondEntered.await(200, TimeUnit.MILLISECONDS));

            handlerRelease.tryEmitValue(List.of(new ConfirmResult(true, pending)));
            assertTrue(continuationEntered.await(5, TimeUnit.SECONDS));
            assertFalse(secondEntered.await(200, TimeUnit.MILLISECONDS));

            continuationRelease.tryEmitValue(
                    AssistantMessage.builder().textContent("first").build());
            firstRun.get(5, TimeUnit.SECONDS);
            secondRun.get(5, TimeUnit.SECONDS);
            assertEquals("first", first.getSlot().getResponseData());
            assertEquals("second", second.getSlot().getResponseData());
            assertNoAgentAttachments(first.getSlot());
            assertNoAgentAttachments(second.getSlot());

            second.process();
            assertEquals(2, second.callCount.get());
            assertNoAgentAttachments(second.getSlot());
        } finally {
            handlerRelease.tryEmitEmpty();
            continuationRelease.tryEmitEmpty();
            pool.shutdownNow();
        }
    }

    @Test
    void invalidConfirmationTimeoutFailsBeforeRuntimeAndAttachmentCreation() throws Exception {
        AgentConfig config = configure(Duration.ofSeconds(5), Duration.ofSeconds(1));
        TestComponent component = component(slot(), null);

        for (Duration invalid : new Duration[]{null, Duration.ZERO, Duration.ofMillis(-1)}) {
            config.getHitl().setConfirmationTimeout(invalid);
            AgentConfigException thrown = assertThrows(
                    AgentConfigException.class, component::process);
            assertTrue(thrown.getMessage().contains("hitl.confirmation-timeout"));
            assertEquals(0, component.buildCount.get());
            assertNoAgentAttachments(component.getSlot());
        }
    }

    @Test
    void missingHandlerCleansDeniedStateThenRemovesAttachment() throws Exception {
        configure(Duration.ofSeconds(5), Duration.ofSeconds(1));
        ToolUseBlock pending = tool();
        TestComponent component = component(slot(), null);
        component.answer = (messages, runtimeContext) -> {
            if (!messages.get(0).getContent().isEmpty()) {
                runtimeContext.get(LiteFlowAgentContext.class).recordConfirmationEvent(
                        new RequireUserConfirmEvent("reply-1", List.of(pending)));
                return Mono.just(askingReply(pending));
            }
            return Mono.just(AssistantMessage.builder().textContent("cleanup").build());
        };

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.PERMISSION, thrown.getErrorType());
        assertEquals(2, component.callCount.get());
        assertNoAgentAttachments(component.getSlot());
    }

    @Test
    void ordinaryAbstractPathRetainsExistingRuntimeTimeoutCancellation() throws Exception {
        configure(Duration.ofMillis(25), Duration.ofSeconds(1));
        OrdinaryComponent component = new OrdinaryComponent(slot());

        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.TIMEOUT, thrown.getErrorType());
        assertTrue(thrown.getMessage().contains("runtime timeout"));
        assertTrue(component.context.isCancelled());
        assertNoAgentAttachments(component.getSlot());
        component.close();
    }

    @Test
    void everyHitlFailurePathRemovesItsSlotAttachment() throws Exception {
        AgentConfig config = configure(Duration.ofMillis(500), Duration.ofMillis(50));

        TestComponent protocol = askingComponent(allowAll(), false,
                Mono.just(AssistantMessage.builder().textContent("cleanup").build()));
        assertFailureAndNoAttachment(protocol, AgentInvocationErrorType.PERMISSION);

        TestComponent invalidResult = askingComponent(
                (event, context) -> Mono.just(List.of()), true,
                Mono.just(AssistantMessage.builder().textContent("cleanup").build()));
        assertFailureAndNoAttachment(invalidResult, AgentInvocationErrorType.PERMISSION);

        TestComponent handlerError = askingComponent(
                (event, context) -> Mono.error(new IllegalStateException("handler failed")),
                true,
                Mono.just(AssistantMessage.builder().textContent("cleanup").build()));
        assertFailureAndNoAttachment(handlerError, AgentInvocationErrorType.PERMISSION);

        TestComponent handlerTimeout = askingComponent(
                (event, context) -> Mono.never(), true,
                Mono.just(AssistantMessage.builder().textContent("cleanup").build()));
        assertFailureAndNoAttachment(handlerTimeout, AgentInvocationErrorType.TIMEOUT);

        IllegalStateException cleanupFailure = new IllegalStateException("cleanup failed");
        TestComponent cleanupError = askingComponent(null, true, Mono.error(cleanupFailure));
        AgentInvocationException cleanupThrown = assertFailureAndNoAttachment(
                cleanupError, AgentInvocationErrorType.PERMISSION);
        assertTrue(List.of(cleanupThrown.getSuppressed()).contains(cleanupFailure));

        config.getHitl().setFailOnDeniedTool(true);
        TestComponent denied = askingComponent(
                (event, context) -> Mono.just(List.of(
                        new ConfirmResult(false, event.getToolCalls().get(0)))),
                true,
                Mono.just(AssistantMessage.builder().textContent("cleanup").build()));
        assertFailureAndNoAttachment(denied, AgentInvocationErrorType.PERMISSION);

        config.getHitl().setFailOnDeniedTool(false);
        config.getRuntime().setTimeout(Duration.ofMillis(50));
        TestComponent initialTimeout = component(slot(), allowAll());
        initialTimeout.answer = (messages, runtimeContext) -> Mono.never();
        assertFailureAndNoAttachment(initialTimeout, AgentInvocationErrorType.TIMEOUT);
    }

    private TestComponent askingComponent(
            AgentConfirmationHandler handler,
            boolean recordEvent,
            Mono<Msg> continuation) {
        ToolUseBlock pending = tool();
        TestComponent component = component(slot(), handler);
        component.answer = (messages, runtimeContext) -> {
            if (!messages.get(0).getContent().isEmpty()) {
                if (recordEvent) {
                    runtimeContext.get(LiteFlowAgentContext.class).recordConfirmationEvent(
                            new RequireUserConfirmEvent("reply-1", List.of(pending)));
                }
                return Mono.just(askingReply(pending));
            }
            return continuation;
        };
        return component;
    }

    private static AgentInvocationException assertFailureAndNoAttachment(
            TestComponent component,
            AgentInvocationErrorType expectedType) throws Exception {
        AgentInvocationException thrown = assertThrows(
                AgentInvocationException.class, component::process);
        assertEquals(expectedType, thrown.getErrorType());
        assertNoAgentAttachments(component.getSlot());
        return thrown;
    }

    private static AgentConfirmationHandler allowAll() {
        return (event, context) -> Mono.just(event.getToolCalls().stream()
                .map(tool -> new ConfirmResult(true, tool))
                .toList());
    }

    private TestComponent component(Slot slot, AgentConfirmationHandler handler) {
        TestComponent component = new TestComponent(slot, handler);
        component.setNodeId("shared-agent");
        components.add(component);
        return component;
    }

    private static AgentConfig configure(Duration runtime, Duration confirmation) {
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace("hitl-test");
        agent.getRuntime().setDefaultUserId("user-1");
        agent.getRuntime().setTimeout(runtime);
        agent.getHitl().setConfirmationTimeout(confirmation);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agent;
    }

    private static Slot slot() {
        Slot slot = new Slot();
        slot.setChainId("chain-1");
        slot.setConversationId("conversation-1");
        return slot;
    }

    private static ToolUseBlock tool() {
        return new ToolUseBlock("tool-1", "write", Map.of("path", "file.txt"), null,
                Map.of(), ToolCallState.ASKING);
    }

    private static Msg askingReply(ToolUseBlock tool) {
        return AssistantMessage.builder()
                .content(tool)
                .metadata(Map.of(Msg.METADATA_CONFIRM_REQUEST_REPLY_ID, "reply-1"))
                .generateReason(GenerateReason.PERMISSION_ASKING)
                .build();
    }

    @SuppressWarnings("unchecked")
    private static void assertNoAgentAttachments(Slot slot) throws Exception {
        Field field = Slot.class.getDeclaredField("metaDataMap");
        field.setAccessible(true);
        Map<String, Object> metadata = (ConcurrentHashMap<String, Object>) field.get(slot);
        Set<String> keys = metadata.keySet().stream()
                .filter(key -> key.startsWith(LiteFlowAgentContext.SLOT_ATTACHMENT_PREFIX))
                .collect(Collectors.toSet());
        assertEquals(Set.of(), keys);
    }

    @FunctionalInterface
    private interface CallAnswer {
        Mono<Msg> answer(List<Msg> messages, RuntimeContext runtimeContext);
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final Slot slot;
        private final AgentConfirmationHandler handler;
        private final ReActAgent agent = mock(ReActAgent.class);
        private final AtomicInteger buildCount = new AtomicInteger();
        private final AtomicInteger callCount = new AtomicInteger();
        private CallAnswer answer;

        private TestComponent(Slot slot, AgentConfirmationHandler handler) {
            this.slot = slot;
            this.handler = handler;
            when(agent.call(anyList(), any(RuntimeContext.class))).thenAnswer(invocation -> {
                callCount.incrementAndGet();
                return answer.answer(invocation.getArgument(0), invocation.getArgument(1));
            });
        }

        @Override public Slot getSlot() { return slot; }
        @Override protected ModelSpec<?> model() { throw new AssertionError("unused"); }
        @Override protected String systemPrompt() { return "system"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "question"; }
        @Override protected AgentConfirmationHandler confirmationHandler() { return handler; }

        @Override
        protected ReActAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            buildCount.incrementAndGet();
            InMemoryAgentStateStore delegate = new InMemoryAgentStateStore();
            return new ReActAgentRuntime(
                    agent,
                    new GuardedNamespacedAgentStateStore(
                            delegate, buildContext.agentNamespace()),
                    new ResolvedAgentStateStore(delegate, false),
                    List.of());
        }
    }

    private static final class OrdinaryComponent extends AbstractAgentComponent<SimpleRuntime> {
        private final Slot slot;
        private LiteFlowAgentContext context;

        private OrdinaryComponent(Slot slot) {
            this.slot = slot;
            setNodeId("ordinary-agent");
        }

        @Override public Slot getSlot() { return slot; }
        @Override protected SimpleRuntime buildRuntime(AgentRuntimeBuildContext context) {
            return new SimpleRuntime();
        }
        @Override protected Mono<Msg> invokeRuntime(
                SimpleRuntime runtime,
                List<Msg> input,
                AgentOutputSpec output,
                RuntimeContext runtimeContext,
                LiteFlowAgentContext liteflowContext) {
            context = liteflowContext;
            return Mono.never();
        }
        @Override protected String systemPrompt() { return "system"; }
        @Override protected String userPrompt(LiteFlowAgentContext context) { return "question"; }
    }

    private static final class SimpleRuntime implements AutoCloseable {
        @Override public void close() { }
    }
}
