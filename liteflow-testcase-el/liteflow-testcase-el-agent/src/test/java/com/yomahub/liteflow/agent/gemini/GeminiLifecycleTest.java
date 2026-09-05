package com.yomahub.liteflow.agent.gemini;

import com.yomahub.liteflow.agent.component.AgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.AgentRuntime;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;
import io.agentscope.extensions.model.gemini.GeminiChatModel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiLifecycleTest {

    private static final String AGENT_NAMESPACE =
            "lf-bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb";

    @Test
    void realRuntimeClosesOwningGeminiDelegateExactlyOnce() {
        CountingGeminiChatModel delegate = new CountingGeminiChatModel();
        OwnedGeminiModel owned = new OwnedGeminiModel(delegate);
        TestComponent component = new TestComponent(owned, null);

        AgentRuntime runtime = component.runtime();
        runtime.close();
        runtime.close();

        assertEquals(1, delegate.closeCount.get());
        assertEquals(delegate.getModelName(), owned.getModelName());
        assertEquals(delegate.getContextWindowSize(), owned.getContextWindowSize());
        assertEquals(
                delegate.supportsNativeStructuredOutput(),
                owned.supportsNativeStructuredOutput());
        assertEquals(
                delegate.supportsNativeStructuredOutputWithTools(),
                owned.supportsNativeStructuredOutputWithTools());
    }

    @Test
    void realComponentBuildRollbackClosesOwningGeminiDelegateExactlyOnce() {
        CountingGeminiChatModel delegate = new CountingGeminiChatModel();
        OwnedGeminiModel owned = new OwnedGeminiModel(delegate);
        RuntimeException buildFailure = new RuntimeException("gemini build failed");
        TestComponent component = new TestComponent(owned, buildFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class, component::runtime);

        assertSame(buildFailure, thrown);
        assertEquals(1, delegate.closeCount.get());
    }

    private static final class TestComponent extends AgentComponent {
        private final Model model;
        private final RuntimeException buildFailure;

        private TestComponent(Model model, RuntimeException buildFailure) {
            this.model = model;
            this.buildFailure = buildFailure;
        }

        private AgentRuntime runtime() {
            AgentConfig config = new AgentConfig();
            config.getStateStore().setJsonRoot("target/agent-state");
            config.getRuntime().setNamespace("gemini-lifecycle-test");
            return buildRuntime(new AgentRuntimeBuildContext(
                    config, "gemini-agent", "gemini-key", AGENT_NAMESPACE));
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
        protected String systemPrompt() {
            return "Gemini lifecycle test";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "offline";
        }

        @Override
        protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder) {
            if (buildFailure != null) {
                throw buildFailure;
            }
            return builder;
        }
    }

    private static final class CountingGeminiChatModel extends GeminiChatModel {
        private final AtomicInteger closeCount = new AtomicInteger();

        private CountingGeminiChatModel() {
            super(
                    "offline-key",
                    "gemini-offline",
                    false,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            super.close();
        }
    }
}
