package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentInvocationErrorType;
import com.yomahub.liteflow.agent.exception.AgentInvocationException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.slot.Slot;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ModelUtils;
import io.agentscope.core.model.ToolSchema;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentRetryFallbackTest {

    private LiteflowConfig previousConfig;
    private TestComponent component;

    @AfterEach
    void restoreConfigAndClose() {
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
    void maxRetriesIsPassedToTheRealModelThroughGenerateOptions() throws Exception {
        AgentConfig config = configure();
        config.setExecutionTimeout(Duration.ofSeconds(1));
        CountingModel primary = CountingModel.reply("primary", "primary reply");
        component = component(primary, null);
        component.maxRetries = 6;

        component.process();

        assertEquals("primary reply", component.slot.getResponseData());
        assertEquals(1, primary.callCount.get());
        GenerateOptions options = primary.options.get(0);
        assertNotNull(options);
        assertNotNull(options.getExecutionConfig());
        assertEquals(6, options.getExecutionConfig().getMaxAttempts());
    }

    @Test
    void nativeFallbackIsSelectedOnceAfterPrimaryStreamFailure() throws Exception {
        configure();
        RuntimeException primaryFailure = new RuntimeException("primary failed");
        CountingModel primary = CountingModel.failure("primary", primaryFailure);
        CountingModel fallback = CountingModel.reply("fallback", "fallback reply");
        component = component(primary, fallback);
        component.maxRetries = 1;

        component.process();

        assertEquals("fallback reply", component.slot.getResponseData());
        assertEquals(1, primary.callCount.get());
        assertEquals(1, fallback.callCount.get());
    }

    @Test
    void nativeFallbackFailurePropagatesWithoutRetainingPrimaryError() {
        configure();
        RuntimeException primaryFailure = new RuntimeException("primary failed");
        IllegalStateException fallbackFailure = new IllegalStateException("fallback failed");
        CountingModel primary = CountingModel.failure("primary", primaryFailure);
        CountingModel fallback = CountingModel.failure("fallback", fallbackFailure);
        component = component(primary, fallback);
        component.maxRetries = 1;

        RuntimeException thrown = assertThrows(RuntimeException.class, component::process);

        assertSame(fallbackFailure, rootRuntimeCause(thrown));
        assertTrue(Arrays.stream(fallbackFailure.getSuppressed())
                .noneMatch(suppressed -> suppressed == primaryFailure),
                "AgentScope native fallback discards the primary failure");
        assertEquals(1, primary.callCount.get());
        assertEquals(1, fallback.callCount.get());
    }

    @Test
    void executionDeadlineCancelsSourceStopsPendingRetryAndMapsOuterTimeout() throws Exception {
        AgentConfig config = configure();
        config.setExecutionTimeout(Duration.ofMillis(80));
        CountingModel primary = CountingModel.executionAwareNever("primary");
        component = component(primary, null);
        component.maxRetries = 3;
        component.modelExecutionConfig = ExecutionConfig.builder()
                .timeout(Duration.ofMillis(20))
                .maxAttempts(3)
                .build();

        AgentInvocationException thrown = assertTimeoutPreemptively(
                Duration.ofSeconds(2),
                () -> assertThrows(AgentInvocationException.class, component::process));

        assertEquals(AgentInvocationErrorType.TIMEOUT, thrown.getErrorType());
        assertInstanceOf(java.util.concurrent.TimeoutException.class, thrown.getCause());
        assertTrue(primary.cancellation.await(1, TimeUnit.SECONDS));
        assertEquals(1, primary.callCount.get());
        assertEquals(1, primary.subscriptionCount.get(),
                "outer deadline must cancel before the delayed retry subscribes again");
    }

    private TestComponent component(CountingModel primary, CountingModel fallback) {
        Slot slot = new Slot();
        slot.setChainId("retry-chain");
        slot.setConversationId("conversation-1");
        slot.putRequestId("request-1");
        TestComponent testComponent = new TestComponent(slot, primary, fallback);
        testComponent.setNodeId("retry-agent");
        return testComponent;
    }

    private AgentConfig configure() {
        AgentConfig agentConfig = new AgentConfig();
        agentConfig.getHarness().getLocal().setWorkspaceRoot(java.nio.file.Path.of("target", "harness-tests", java.util.UUID.randomUUID().toString()).toAbsolutePath().toString());
        agentConfig.getSessionStore().setJsonWorkspaceRoot(agentConfig.getHarness().getLocal().getWorkspaceRoot() + "/records");
        agentConfig.getSessionStore().setJsonRoot("target/agent-state");
        agentConfig.setApplicationName("retry-test");
        agentConfig.setExecutionTimeout(Duration.ofSeconds(1));
        LiteflowConfig config = new LiteflowConfig();
        config.setAgent(agentConfig);
        LiteflowConfigGetter.setLiteflowConfig(config);
        return agentConfig;
    }

    private static RuntimeException rootRuntimeCause(RuntimeException failure) {
        Throwable current = failure;
        while (current.getCause() != null && current.getCause() != current) {
            current = current.getCause();
        }
        return (RuntimeException) current;
    }

    private static final class TestComponent extends HarnessAgentComponent {
        // This fixture exercises non-Shell behavior; opt out of the enabled-by-default tool.
        @Override protected boolean enableShellTool() { return false; }
        @Override protected io.agentscope.harness.agent.HarnessAgent.Builder customizeHarness(
                io.agentscope.harness.agent.HarnessAgent.Builder builder) {
            return builder.disableMemoryHooks().disableCompaction().disableDefaultWorkspaceSkills();
        }
        private final Slot slot;
        private final Model primary;
        private final Model fallback;
        private int maxRetries = 1;
        private ExecutionConfig modelExecutionConfig;

        private TestComponent(Slot slot, Model primary, Model fallback) {
            this.slot = slot;
            this.primary = primary;
            this.fallback = fallback;
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
            return primary;
        }

        @Override
        protected Model fallbackModel() {
            return fallback;
        }

        @Override
        protected int maxRetries() {
            return maxRetries;
        }

        @Override
        protected ExecutionConfig modelExecutionConfig() {
            return modelExecutionConfig;
        }

        @Override
        protected String systemPrompt() {
            return "Answer directly.";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }
    }

    private static final class CountingModel implements Model, AutoCloseable {
        private enum Behavior { REPLY, FAILURE, EXECUTION_AWARE_NEVER }

        private final String name;
        private final Behavior behavior;
        private final String reply;
        private final Throwable failure;
        private final AtomicInteger callCount = new AtomicInteger();
        private final AtomicInteger subscriptionCount = new AtomicInteger();
        private final CountDownLatch cancellation = new CountDownLatch(1);
        private final List<GenerateOptions> options = new CopyOnWriteArrayList<>();

        private CountingModel(String name, Behavior behavior, String reply, Throwable failure) {
            this.name = name;
            this.behavior = behavior;
            this.reply = reply;
            this.failure = failure;
        }

        private static CountingModel reply(String name, String reply) {
            return new CountingModel(name, Behavior.REPLY, reply, null);
        }

        private static CountingModel failure(String name, Throwable failure) {
            return new CountingModel(name, Behavior.FAILURE, null, failure);
        }

        private static CountingModel executionAwareNever(String name) {
            return new CountingModel(name, Behavior.EXECUTION_AWARE_NEVER, null, null);
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions generateOptions) {
            callCount.incrementAndGet();
            options.add(generateOptions);
            if (behavior == Behavior.FAILURE) {
                return Flux.error(failure);
            }
            if (behavior == Behavior.EXECUTION_AWARE_NEVER) {
                Flux<ChatResponse> source = Flux.<ChatResponse>never()
                        .doOnSubscribe(ignored -> subscriptionCount.incrementAndGet())
                        .doOnCancel(cancellation::countDown);
                return ModelUtils.applyTimeoutAndRetry(
                        source, generateOptions, null, name, "test");
            }
            ContentBlock content = TextBlock.builder().text(reply).build();
            return Flux.just(ChatResponse.builder()
                    .content(List.of(content))
                    .finishReason("stop")
                    .build());
        }

        @Override
        public String getModelName() {
            return name;
        }

        @Override
        public void close() {
        }
    }
}
