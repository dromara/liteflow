package com.yomahub.liteflow.test.agent.feature.lifecycle;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.hitl.AgentConfirmationHandler;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import org.junit.jupiter.api.Test;
import org.noear.solon.core.AppContext;
import org.noear.solon.core.Props;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SolonAgentLifecycleTest {

    @Test
    void stoppingFreshAppContextClosesTheRegisteredAgentRuntimeExactlyOnce() throws Exception {
        AgentConfig agent = new AgentConfig();
        agent.getHarness().getLocal().setWorkspaceRoot(java.nio.file.Path.of("target", "harness-tests", java.util.UUID.randomUUID().toString()).toAbsolutePath().toString());
        agent.getSessionStore().setJsonWorkspaceRoot(agent.getHarness().getLocal().getWorkspaceRoot() + "/records");
        agent.getSessionStore().setJsonRoot("target/agent-state");
        agent.setApplicationName("solon-lifecycle-test");
        LiteflowConfig liteflowConfig = new LiteflowConfig();
        liteflowConfig.setAgent(agent);
        LiteflowConfigGetter.setLiteflowConfig(liteflowConfig);

        AppContext context = new AppContext(getClass().getClassLoader(), new Props());
        ClosingFakeModel model = new ClosingFakeModel();
        LifecycleAgentComponent component = new LifecycleAgentComponent(model);
        context.wrapAndPut(LifecycleAgentComponent.class, component);
        context.start();

        try {
            component.process();
            assertEquals(0, model.closeCount.get());

            context.stop();
            context.stop();

            assertEquals(1, component.runtimeBuildCount.get());
            assertEquals(1, model.closeCount.get());
        } finally {
            context.stop();
            LiteflowConfigGetter.clean();
        }
    }

    private static final class LifecycleAgentComponent extends HarnessAgentComponent {
        private final Slot slot = new Slot();
        private final Model model;
        private final AtomicInteger runtimeBuildCount = new AtomicInteger();

        private LifecycleAgentComponent(Model model) {
            this.model = model;
            slot.setChainId("solon-lifecycle-chain");
            slot.setConversationId("solon-lifecycle-conversation");
            slot.putRequestId("solon-lifecycle-request");
            setNodeId("solon-lifecycle-agent");
        }

        @Override
        public Slot getSlot() {
            return slot;
        }

        @Override
        protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
            runtimeBuildCount.incrementAndGet();
            return super.buildRuntime(buildContext);
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
            return "Solon lifecycle test";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }

        @Override
        protected AgentConfirmationHandler confirmationHandler() {
            return (event, context) -> Mono.just(List.of());
        }
    }

    private static final class ClosingFakeModel implements Model, AutoCloseable {
        private final AtomicInteger closeCount = new AtomicInteger();

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            ContentBlock content = TextBlock.builder().text("fake reply").build();
            return Flux.just(ChatResponse.builder()
                    .content(List.of(content))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return "solon-fake-model";
        }

        @Override
        public void close() {
            closeCount.incrementAndGet();
        }
    }
}
