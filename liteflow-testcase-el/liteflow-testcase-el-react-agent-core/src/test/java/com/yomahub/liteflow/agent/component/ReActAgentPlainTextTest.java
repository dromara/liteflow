package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.guard.AgentInvocationGuard;
import com.yomahub.liteflow.agent.guard.AgentInvocationKey;
import com.yomahub.liteflow.agent.guard.AgentInvocationLease;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeHandle;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.AgentInvocationGuardMode;
import com.yomahub.liteflow.slot.Slot;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.spi.local.LocalContextAware;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.model.Model;
import io.agentscope.core.state.InMemoryAgentStateStore;
import io.agentscope.core.state.State;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReActAgentPlainTextTest {

    private LiteflowConfig previousConfig;
    private TestComponent component;
    private Field contextAwareField;
    private Object previousContextAware;

    @AfterEach
    void restoreGlobalConfigAndCloseComponent() throws IllegalAccessException {
        if (component != null) {
            component.close();
        }
        if (contextAwareField != null) {
            contextAwareField.set(null, previousContextAware);
        }
        if (previousConfig != null) {
            LiteflowConfigGetter.setLiteflowConfig(previousConfig);
        } else {
            LiteflowConfigGetter.clean();
        }
    }

    @Test
    void realAgentScopeCallWritesPlainTextToSlotAndPropagatesRuntimeContext() throws Exception {
        configureAgent("plain-text-test", "test-user");
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        ScriptedChatModel model = new ScriptedChatModel("literal scripted reply");
        component = new TestComponent(slot, model);
        component.setNodeId("plain-agent");

        component.process();
        component.process();

        assertEquals("literal scripted reply", slot.getResponseData());
        assertEquals(2, model.callCount());
        assertEquals(1, component.modelBuildCount.get());
        Msg firstUserInput = model.inputAt(0).stream()
                .filter(message -> "hello from slot".equals(message.getTextContent()))
                .findFirst()
                .orElseThrow();
        assertEquals("hello from slot", firstUserInput.getTextContent());

        RuntimeContext runtimeContext = model.runtimeContextAt(0);
        assertEquals("test-user", runtimeContext.getUserId());
        assertTrue(runtimeContext.getSessionId().matches("lf-[0-9a-f]{64}"));
        assertSame(slot, runtimeContext.get(Slot.class));
        assertEquals(runtimeContext.getSessionId(), model.runtimeContextAt(1).getSessionId());
    }

    @Test
    void invalidRuntimeConfigurationFailsBeforeModelInvocation() {
        configureAgent(" ", "test-user");
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        ScriptedChatModel model = new ScriptedChatModel("must-not-run");
        component = new TestComponent(slot, model);
        component.setNodeId("plain-agent");

        assertThrows(RuntimeException.class, component::process);
        assertEquals(0, model.callCount());
        assertEquals(0, component.modelBuildCount.get());
    }

    @Test
    void nullZeroOrNegativeRuntimeTimeoutFailsBeforeModelInvocation() {
        AgentConfig agentConfig = configureAgent("plain-text-test", "test-user");
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        ScriptedChatModel model = new ScriptedChatModel("must-not-run");
        component = new TestComponent(slot, model);
        component.setNodeId("plain-agent");

        for (Duration invalid : new Duration[]{null, Duration.ZERO, Duration.ofMillis(-1)}) {
            agentConfig.getRuntime().setTimeout(invalid);
            AgentConfigException thrown = assertThrows(
                    AgentConfigException.class, component::process);
            assertTrue(thrown.getMessage().contains("runtime.timeout"));
        }
        assertEquals(0, model.callCount());
        assertEquals(0, component.modelBuildCount.get());
    }

    @Test
    void runtimeTimeoutCancelsCallClassifiesFailureAndReleasesGuardForRetry() throws Exception {
        AgentConfig agentConfig = configureAgent("plain-text-test", "test-user");
        agentConfig.getRuntime().setTimeout(Duration.ofMillis(25));
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        CountDownLatch cancellation = new CountDownLatch(1);
        ScriptedChatModel model = ScriptedChatModel.neverThenReply(
                "reply after timeout", cancellation);
        component = new TestComponent(slot, model);
        component.setNodeId("plain-agent");

        AgentInvocationException thrown = assertTimeoutPreemptively(
                Duration.ofSeconds(1),
                () -> assertThrows(AgentInvocationException.class, component::process));
        assertEquals(AgentInvocationErrorType.TIMEOUT, thrown.getErrorType());
        assertInstanceOf(TimeoutException.class, thrown.getCause());
        assertTrue(thrown.getMessage().contains("runtime timeout"));
        assertTrue(cancellation.await(5, TimeUnit.SECONDS));

        assertTimeoutPreemptively(Duration.ofSeconds(1), component::process);
        assertEquals("reply after timeout", slot.getResponseData());
        assertEquals(2, model.callCount());
        assertEquals(1, component.modelBuildCount.get());
    }

    @Test
    void upstreamTimeoutExceptionIsPreservedWithoutFrameworkDeadlineClassification() {
        AgentConfig agentConfig = configureAgent("plain-text-test", "test-user");
        agentConfig.getRuntime().setTimeout(Duration.ofSeconds(1));
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        TimeoutException upstreamFailure = new TimeoutException("model-owned timeout");
        ScriptedChatModel model = ScriptedChatModel.immediateFailure(upstreamFailure);
        component = new TestComponent(slot, model);
        component.setNodeId("plain-agent");

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertFalse(thrown instanceof AgentInvocationException);
        assertSame(upstreamFailure, thrown.getCause());
        assertEquals(1, model.callCount());
    }

    @Test
    void completedCallCleanupDoesNotEraseFailureRecordedAfterLeaseRelease() throws Exception {
        AgentConfig agentConfig = configureAgent("plain-text-test", "test-user");
        agentConfig.getInvocationGuard().setMode(AgentInvocationGuardMode.BEAN);
        agentConfig.getInvocationGuard().setBeanName("late-failure-guard");
        Slot slot = new Slot();
        slot.setChainId("plain-chain");
        slot.setConversationId("conversation-7");
        ScriptedChatModel model = new ScriptedChatModel("completed reply");
        LateFailingStore delegate = new LateFailingStore();
        AgentStateStoreResolver resolver = ignored ->
                new ResolvedAgentStateStore(delegate, false);
        component = new TestComponent(slot, model, resolver);
        component.setNodeId("plain-agent");
        RuntimeException lateFailure = new RuntimeException("next invocation load failure");
        LateFailureOnReleaseGuard guard = new LateFailureOnReleaseGuard(
                component, delegate, model, lateFailure);
        installContextAware(new SingleBeanContextAware("late-failure-guard", guard));

        component.process();

        RuntimeContext runtimeContext = model.runtimeContextAt(0);
        GuardedNamespacedAgentStateStore store = component.runtimeStateStore();
        assertSame(lateFailure, store.takeLoadFailure(
                runtimeContext.getUserId(), runtimeContext.getSessionId()).orElseThrow());
        assertTrue(guard.released.get());
    }

    private AgentConfig configureAgent(String namespace, String defaultUserId) {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getStateStore().setJsonRoot("target/agent-state");
        agentConfig.getRuntime().setNamespace(namespace);
        agentConfig.getRuntime().setDefaultUserId(defaultUserId);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agentConfig);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agentConfig;
    }

    private void installContextAware(LocalContextAware contextAware) throws Exception {
        contextAwareField = ContextAwareHolder.class.getDeclaredField("contextAware");
        contextAwareField.setAccessible(true);
        previousContextAware = contextAwareField.get(null);
        contextAwareField.set(null, contextAware);
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final Slot slot;
        private final ScriptedChatModel scriptedModel;
        private final AgentStateStoreResolver stateStoreResolver;
        private final AtomicInteger modelBuildCount = new AtomicInteger();

        private TestComponent(Slot slot, ScriptedChatModel scriptedModel) {
            this(slot, scriptedModel, null);
        }

        private TestComponent(
                Slot slot,
                ScriptedChatModel scriptedModel,
                AgentStateStoreResolver stateStoreResolver) {
            this.slot = slot;
            this.scriptedModel = scriptedModel;
            this.stateStoreResolver = stateStoreResolver;
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
            modelBuildCount.incrementAndGet();
            return scriptedModel;
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            return stateStoreResolver == null
                    ? super.stateStoreResolver()
                    : stateStoreResolver;
        }

        @Override
        protected String systemPrompt() {
            return "Answer with the scripted text.";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello from slot";
        }

        private GuardedNamespacedAgentStateStore runtimeStateStore() {
            try {
                Field handleField = AbstractAgentComponent.class.getDeclaredField("runtimeHandle");
                handleField.setAccessible(true);
                AgentRuntimeHandle<?> handle = (AgentRuntimeHandle<?>) handleField.get(this);
                Field runtimeField = AgentRuntimeHandle.class.getDeclaredField("runtime");
                runtimeField.setAccessible(true);
                return ((ReActAgentRuntime) runtimeField.get(handle)).stateStore();
            } catch (ReflectiveOperationException failure) {
                throw new AssertionError(failure);
            }
        }
    }

    private static final class LateFailingStore extends InMemoryAgentStateStore {
        private RuntimeException failure;

        @Override
        public <T extends State> Optional<T> get(
                String userId, String sessionId, String key, Class<T> type) {
            if (failure != null) {
                throw failure;
            }
            return super.get(userId, sessionId, key, type);
        }
    }

    private static final class LateFailureOnReleaseGuard implements AgentInvocationGuard {
        private final TestComponent component;
        private final LateFailingStore delegate;
        private final ScriptedChatModel model;
        private final RuntimeException lateFailure;
        private final AtomicBoolean released = new AtomicBoolean();

        private LateFailureOnReleaseGuard(
                TestComponent component,
                LateFailingStore delegate,
                ScriptedChatModel model,
                RuntimeException lateFailure) {
            this.component = component;
            this.delegate = delegate;
            this.model = model;
            this.lateFailure = lateFailure;
        }

        @Override
        public AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout) {
            return new AgentInvocationLease() {
                @Override
                public AgentInvocationKey key() {
                    return key;
                }

                @Override
                public void close() {
                    if (!released.compareAndSet(false, true)) {
                        return;
                    }
                    RuntimeContext runtimeContext = model.runtimeContextAt(0);
                    GuardedNamespacedAgentStateStore store = component.runtimeStateStore();
                    delegate.failure = lateFailure;
                    assertSame(lateFailure, assertThrows(RuntimeException.class,
                            () -> store.get(
                                    runtimeContext.getUserId(),
                                    runtimeContext.getSessionId(),
                                    "state",
                                    UserMessage.class)));
                }
            };
        }
    }

    private static final class SingleBeanContextAware extends LocalContextAware {
        private final String beanName;
        private final Object bean;

        private SingleBeanContextAware(String beanName, Object bean) {
            this.beanName = beanName;
            this.bean = bean;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T getBean(String name) {
            return beanName.equals(name) ? (T) bean : null;
        }
    }
}
