package com.yomahub.liteflow.agent.component;

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

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    private void configureAgent(String namespace, String defaultUserId) {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getRuntime().setNamespace(namespace);
        agentConfig.getRuntime().setDefaultUserId(defaultUserId);
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agentConfig);
        LiteflowConfigGetter.setLiteflowConfig(config);
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
