package com.yomahub.liteflow.agent.a2a;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.guard.LocalAgentInvocationGuard;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;
import io.agentscope.core.a2a.agent.A2aAgentConfig;
import io.agentscope.core.a2a.agent.card.AgentCardResolver;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class A2aAgentComponentTest {

    private Object previousConfig;
    private Object previousContextAware;
    private RecordingGuard guard;

    @BeforeEach
    void setUp() throws Exception {
        Field configField = field(LiteflowConfigGetter.class, "liteflowConfig");
        Field contextField = field(ContextAwareHolder.class, "contextAware");
        previousConfig = configField.get(null);
        previousContextAware = contextField.get(null);

        guard = new RecordingGuard();
        AgentConfig agent = new AgentConfig();
        agent.getRuntime().setNamespace("a2a-component-test");
        agent.getRuntime().setDefaultUserId("user-3");
        agent.getRuntime().setTimeout(Duration.ofSeconds(2));
        agent.getInvocationGuard().setMode(AgentInvocationGuardMode.BEAN);
        agent.getInvocationGuard().setBeanName("a2a-test-guard");
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agent);
        configField.set(null, config);
        contextField.set(null, new GuardContextAware(guard));
    }

    @AfterEach
    void tearDown() throws Exception {
        field(LiteflowConfigGetter.class, "liteflowConfig").set(null, previousConfig);
        field(ContextAwareHolder.class, "contextAware").set(null, previousContextAware);
    }

    @Test
    void consecutiveCallsUseDistinctRemoteInvocationsAndExactMetadata() throws Exception {
        RecordingRuntimeFactory factory = new RecordingRuntimeFactory();
        Slot slot = slot("conversation-7");
        TestComponent component = new TestComponent(slot, factory);

        component.process();
        String firstReply = slot.getResponseData();
        component.process();
        String secondReply = slot.getResponseData();

        assertEquals(1, factory.runtimeBuilds.get());
        assertEquals(2, factory.createdAgents.get());
        assertEquals(List.of("reply-1", "reply-2"), List.of(firstReply, secondReply));
        assertEquals("conversation-7", component.lastContext.getConversationId());
        assertEquals(Map.of(
                        "liteflow.userId", "user-3",
                        "liteflow.conversationId", "conversation-7",
                        "liteflow.agentKey", "node-a",
                        "liteflow.traceId", "request-9"),
                factory.requests.get(1).message().getMetadata());
        assertEquals(0, guard.active.get());
        component.close();
        assertEquals(1, factory.runtimeCloses.get());
    }

    @Test
    void structuredOutputFailsBeforeSendingTheRemoteRequest() {
        RecordingRuntimeFactory factory = new RecordingRuntimeFactory();
        TestComponent component = new TestComponent(slot("structured"), factory);
        component.structuredOutput = true;

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, component::process);

        assertEquals("A2A client supports TEXT output only", failure.getMessage());
        assertEquals(0, factory.createdAgents.get());
        assertEquals(0, guard.active.get());
        component.close();
    }

    @Test
    void remoteErrorReleasesTheLeaseAndTheSameConversationCanRetry() throws Exception {
        RecordingRuntimeFactory factory = new RecordingRuntimeFactory();
        RuntimeException remote = new RuntimeException("remote failed");
        factory.response.set(request -> Mono.error(remote));
        Slot slot = slot("remote-error");
        TestComponent component = new TestComponent(slot, factory);

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertTrue(hasCause(thrown, remote));
        assertEquals(0, guard.active.get());
        factory.response.set(request -> Mono.just(new UserMessage("recovered")));
        component.process();
        assertEquals("recovered", slot.getResponseData());
        assertEquals(0, guard.active.get());
        component.close();
    }

    @Test
    void coreDeadlineCancelsDefaultRuntimeInterruptsOnceAndReleasesLeaseForRetry() throws Exception {
        LiteflowConfigGetter.get().getAgent().getRuntime().setTimeout(Duration.ofMillis(40));
        AtomicInteger interrupts = new AtomicInteger();
        CountDownLatch cancelled = new CountDownLatch(1);
        AtomicReference<Mono<Msg>> response = new AtomicReference<>(
                Mono.<Msg>never().doOnCancel(cancelled::countDown));
        A2aAgentFactory agents = request -> new A2aAgentHandle() {
            @Override
            public Mono<Msg> call(UserMessage message) {
                return response.get();
            }

            @Override
            public void interrupt() {
                interrupts.incrementAndGet();
            }
        };
        Slot slot = slot("default-runtime-timeout");
        TestComponent component = new TestComponent(
                slot,
                A2aClientRuntimeFactory.defaultFactory(agents));

        AgentInvocationException failure = assertThrows(
                AgentInvocationException.class, component::process);

        assertEquals(AgentInvocationErrorType.TIMEOUT, failure.getErrorType());
        assertTrue(failure.getCause() instanceof java.util.concurrent.TimeoutException);
        assertTrue(failure.getCause().getMessage().startsWith(
                "Agent invocation exceeded runtime timeout"));
        assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        assertEquals(1, interrupts.get());
        assertEquals(0, guard.active.get());
        response.set(Mono.just(new UserMessage("after-timeout")));
        component.process();
        assertEquals("after-timeout", slot.getResponseData());
        assertEquals(1, interrupts.get());
        assertEquals(0, guard.active.get());
        component.close();
    }

    @Test
    void callerCancellationCancelsTheCallAndReleasesTheLease() throws Exception {
        RecordingRuntimeFactory factory = new RecordingRuntimeFactory();
        CountDownLatch subscribed = new CountDownLatch(1);
        CountDownLatch cancelled = new CountDownLatch(1);
        factory.response.set(request -> Mono.<Msg>never()
                .doOnSubscribe(ignored -> subscribed.countDown())
                .doOnCancel(cancelled::countDown));
        TestComponent component = new TestComponent(slot("caller-cancel"), factory);
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            Future<?> call = executor.submit(() -> {
                component.process();
                return null;
            });
            assertTrue(subscribed.await(5, TimeUnit.SECONDS));

            call.cancel(true);

            assertTrue(cancelled.await(5, TimeUnit.SECONDS));
        }
        finally {
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
        }
        assertEquals(0, guard.active.get());
        component.close();
    }

    private static boolean hasCause(Throwable failure, Throwable expected) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current == expected) {
                return true;
            }
        }
        return false;
    }

    private static Slot slot(String conversationId) {
        Slot slot = new Slot();
        slot.setChainId("chain-a");
        slot.setConversationId(conversationId);
        slot.putRequestId("request-9");
        return slot;
    }

    private static Field field(Class<?> owner, String name) throws Exception {
        Field field = owner.getDeclaredField(name);
        field.setAccessible(true);
        return field;
    }

    private static final class TestComponent extends A2aAgentComponent {
        private final Slot slot;
        private final A2aClientRuntimeFactory factory;
        private LiteFlowAgentContext lastContext;
        private boolean structuredOutput;

        private TestComponent(Slot slot, A2aClientRuntimeFactory factory) {
            this.slot = slot;
            this.factory = factory;
            setNodeId("node-a");
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected String remoteAgentName() {
            return "remote-a";
        }

        @Override
        protected AgentCardResolver agentCardResolver() {
            return name -> null;
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            lastContext = context;
            return "hello";
        }

        @Override
        protected A2aClientRuntimeFactory a2aClientRuntimeFactory() {
            return factory;
        }

        @Override
        protected Class<?> structuredOutputType() {
            return structuredOutput ? String.class : null;
        }
    }

    private static final class RecordingRuntimeFactory implements A2aClientRuntimeFactory {
        private final AtomicInteger runtimeBuilds = new AtomicInteger();
        private final AtomicInteger createdAgents = new AtomicInteger();
        private final AtomicInteger runtimeCloses = new AtomicInteger();
        private final List<A2aClientRequest> requests = new ArrayList<>();
        private final AtomicReference<java.util.function.Function<A2aClientRequest, Mono<Msg>>>
                response = new AtomicReference<>(request -> Mono.just(
                        new UserMessage("reply-" + createdAgents.get())));

        @Override
        public A2aClientRuntime create(AgentCardResolver resolver, A2aAgentConfig config) {
            runtimeBuilds.incrementAndGet();
            return new A2aClientRuntime() {
                @Override
                public Mono<Msg> call(A2aClientRequest request) {
                    return Mono.defer(() -> {
                        requests.add(request);
                        createdAgents.incrementAndGet();
                        return response.get().apply(request);
                    });
                }

                @Override
                public void close() {
                    runtimeCloses.incrementAndGet();
                }
            };
        }
    }

    private static final class RecordingGuard implements AgentInvocationGuard {
        private final LocalAgentInvocationGuard delegate = new LocalAgentInvocationGuard();
        private final AtomicInteger active = new AtomicInteger();

        @Override
        public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
            AgentInvocationLease lease = delegate.acquire(key, timeout);
            active.incrementAndGet();
            return new AgentInvocationLease() {
                @Override
                public AgentInvocationKey key() {
                    return lease.key();
                }

                @Override
                public void close() {
                    lease.close();
                    active.decrementAndGet();
                }
            };
        }
    }

    private static final class GuardContextAware extends LocalContextAware {
        private final RecordingGuard guard;

        private GuardContextAware(RecordingGuard guard) {
            this.guard = guard;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getBean(String name) {
            return "a2a-test-guard".equals(name) ? (T) guard : null;
        }

        @Override
        public <T> Map<String, T> getBeansOfType(Class<T> type) {
            return Map.of();
        }
    }
}
