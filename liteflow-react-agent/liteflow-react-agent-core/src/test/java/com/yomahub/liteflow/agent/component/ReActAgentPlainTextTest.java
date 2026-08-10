package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.testsupport.ScriptedChatModel;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.Model;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReActAgentPlainTextTest {

    private LiteflowConfig previousConfig;
    private TestComponent component;

    @AfterEach
    void restoreGlobalConfigAndCloseComponent() {
        if (component != null) {
            component.close();
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

    private AgentConfig configureAgent(String namespace, String defaultUserId) {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getRuntime().setNamespace(namespace);
        agentConfig.getRuntime().setDefaultUserId(defaultUserId);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agentConfig);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agentConfig;
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final Slot slot;
        private final ScriptedChatModel scriptedModel;
        private final AtomicInteger modelBuildCount = new AtomicInteger();

        private TestComponent(Slot slot, ScriptedChatModel scriptedModel) {
            this.slot = slot;
            this.scriptedModel = scriptedModel;
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
        protected String systemPrompt() {
            return "Answer with the scripted text.";
        }

        @Override
        protected String userPrompt() {
            return "hello from slot";
        }
    }
}
