package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.config.ModelConfig;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReActAgentBuilderConfigurationTest {

    private static final String AGENT_NAMESPACE =
            "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void mapsEveryNativeBuilderOptionAndRunsCustomizerLast() {
        CloseableModel defaultModel = new CloseableModel("default", new ArrayList<>());
        CloseableModel fallbackModel = new CloseableModel("fallback", new ArrayList<>());
        CloseableModel routingModel = new CloseableModel("routing", new ArrayList<>());
        ExecutionConfig modelExecution = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(11))
                .build();
        ExecutionConfig toolExecution = ExecutionConfig.builder()
                .timeout(Duration.ofSeconds(12))
                .maxAttempts(1)
                .build();
        PermissionContextState permission = PermissionContextState.builder()
                .mode(PermissionMode.BYPASS)
                .build();
        TestComponent component = new TestComponent(defaultModel);
        component.iterations = 7;
        component.modelExecution = modelExecution;
        component.toolExecution = toolExecution;
        component.retries = 4;
        component.fallback = fallbackModel;
        component.routing = List.of(routingModel);
        component.permission = permission;
        component.stopOnReject = true;
        component.customizer = builder -> builder.maxIters(9).model(defaultModel);

        ReActAgentRuntime runtime = component.runtime(config());
        try {
            ReActAgent agent = runtime.agent();
            assertEquals(9, agent.getReactConfig().maxIters(),
                    "customizer must execute after maxIterations mapping");
            assertTrue(agent.getReactConfig().stopOnReject());
            assertSame(modelExecution, agent.getModelExecutionConfig());
            assertSame(toolExecution, agent.getToolExecutionConfig());
            assertEquals(4, agent.getModelConfig().maxRetries());
            assertSame(fallbackModel, agent.getModelConfig().fallbackModel());
            assertEquals(permission, agent.getPermissionContext());
            assertSame(defaultModel, agent.getModel());
            assertTrue(agent.getMiddlewares().stream()
                    .anyMatch(middleware -> middleware.order() == 1_000));
            assertEquals(-41, component.userMiddlewares.get(0).order(),
                    "registration must not mutate the caller's middleware instance");
            assertEquals(1, component.customizerCalls.get());
        } finally {
            runtime.close();
        }
    }

    @Test
    void runtimeOwnsDefaultFallbackAndRoutingModelsByIdentityExactlyOnce() {
        List<String> closeOrder = new ArrayList<>();
        CloseableModel defaultModel = new CloseableModel("default", closeOrder);
        CloseableModel fallbackModel = new CloseableModel("fallback", closeOrder);
        CloseableModel routingOne = new CloseableModel("routing-1", closeOrder);
        CloseableModel routingTwo = new CloseableModel("routing-2", closeOrder);
        TestComponent component = new TestComponent(defaultModel);
        component.fallback = fallbackModel;
        component.routing = List.of(
                routingOne, defaultModel, routingTwo, fallbackModel, routingOne);

        ReActAgentRuntime runtime = component.runtime(config());
        runtime.close();
        runtime.close();

        assertEquals(1, defaultModel.closeCount.get());
        assertEquals(1, fallbackModel.closeCount.get());
        assertEquals(1, routingOne.closeCount.get());
        assertEquals(1, routingTwo.closeCount.get());
        assertEquals(List.of("routing-2", "routing-1", "fallback", "default"), closeOrder);
    }

    @Test
    void buildFailureClosesAlreadyBuiltOwnedModelsInReverseIdentityOrder() {
        List<String> closeOrder = new ArrayList<>();
        CloseableModel defaultModel = new CloseableModel("default", closeOrder);
        CloseableModel fallbackModel = new CloseableModel("fallback", closeOrder);
        CloseableModel routingOne = new CloseableModel("routing-1", closeOrder);
        CloseableModel routingTwo = new CloseableModel("routing-2", closeOrder);
        TestComponent component = new TestComponent(defaultModel);
        component.fallback = fallbackModel;
        component.routing = List.of(routingOne, defaultModel, routingTwo, fallbackModel);
        RuntimeException buildFailure = new RuntimeException("customizer failed");
        component.customizer = builder -> {
            throw buildFailure;
        };

        RuntimeException thrown = assertThrows(
                RuntimeException.class, () -> component.runtime(config()));

        assertSame(buildFailure, thrown);
        assertEquals(List.of("routing-2", "routing-1", "fallback", "default"), closeOrder);
    }

    @Test
    void customizerCannotSilentlyIntroduceAnUnmanagedModel() {
        List<String> closeOrder = new ArrayList<>();
        CloseableModel defaultModel = new CloseableModel("default", closeOrder);
        CloseableModel unmanaged = new CloseableModel("unmanaged", closeOrder);
        TestComponent component = new TestComponent(defaultModel);
        component.customizer = builder -> builder.model(unmanaged);

        AgentConfigException thrown = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(thrown.getMessage().contains("unmanaged"));
        assertEquals(1, defaultModel.closeCount.get());
        assertEquals(0, unmanaged.closeCount.get(),
                "the customizer remains responsible for unknown escape-hatch resources");
    }

    @Test
    void invalidCountsAndNullExtensionCollectionsFailDuringBuild() {
        CloseableModel model = new CloseableModel("default", new ArrayList<>());
        TestComponent invalidRetries = new TestComponent(model);
        invalidRetries.retries = 0;
        assertThrows(AgentConfigException.class, () -> invalidRetries.runtime(config()));

        TestComponent invalidIterations = new TestComponent(model);
        invalidIterations.iterations = -2;
        assertThrows(AgentConfigException.class, () -> invalidIterations.runtime(config()));

        TestComponent nullRoutes = new TestComponent(model);
        nullRoutes.routing = null;
        assertThrows(AgentConfigException.class, () -> nullRoutes.runtime(config()));

        TestComponent nullMiddleware = new TestComponent(model);
        nullMiddleware.userMiddlewares = null;
        assertThrows(AgentConfigException.class, () -> nullMiddleware.runtime(config()));
    }

    private static AgentConfig config() {
        AgentConfig config = new AgentConfig();
        config.getRuntime().setNamespace("builder-test");
        return config;
    }

    private static final class TestComponent extends ReActAgentComponent {
        private final CloseableModel defaultModel;
        private int iterations = 7;
        private ExecutionConfig modelExecution;
        private ExecutionConfig toolExecution;
        private int retries = ModelConfig.DEFAULT_MAX_RETRIES;
        private Model fallback;
        private List<Model> routing = List.of();
        private PermissionContextState permission;
        private boolean stopOnReject;
        private List<MiddlewareBase> userMiddlewares = List.of(new UserMiddleware());
        private UnaryOperator<ReActAgent.Builder> customizer = UnaryOperator.identity();
        private final AtomicInteger customizerCalls = new AtomicInteger();

        private TestComponent(CloseableModel defaultModel) {
            this.defaultModel = defaultModel;
        }

        private ReActAgentRuntime runtime(AgentConfig config) {
            return buildRuntime(new AgentRuntimeBuildContext(
                    config, "builder-agent", "agent-key", AGENT_NAMESPACE));
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            return defaultModel;
        }

        @Override
        protected String systemPrompt() {
            return "builder test";
        }

        @Override
        protected String userPrompt(LiteFlowAgentContext context) {
            return "hello";
        }

        @Override
        protected int maxIterations() {
            return iterations;
        }

        @Override
        protected ExecutionConfig modelExecutionConfig() {
            return modelExecution;
        }

        @Override
        protected ExecutionConfig toolExecutionConfig() {
            return toolExecution;
        }

        @Override
        protected int maxRetries() {
            return retries;
        }

        @Override
        protected Model fallbackModel() {
            return fallback;
        }

        @Override
        protected List<Model> routingModels() {
            return routing;
        }

        @Override
        protected PermissionContextState permissionContext() {
            return permission;
        }

        @Override
        protected boolean stopOnReject() {
            return stopOnReject;
        }

        @Override
        protected List<MiddlewareBase> middlewares() {
            return userMiddlewares;
        }

        @Override
        protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder) {
            customizerCalls.incrementAndGet();
            return customizer.apply(builder);
        }
    }

    private static final class UserMiddleware implements MiddlewareBase {
        @Override
        public int order() {
            return -41;
        }
    }

    private static final class CloseableModel implements Model, AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final AtomicInteger closeCount = new AtomicInteger();

        private CloseableModel(String name, List<String> closeOrder) {
            this.name = name;
            this.closeOrder = closeOrder;
        }

        @Override
        public Flux<ChatResponse> stream(
                List<Msg> messages, List<ToolSchema> tools, GenerateOptions options) {
            ContentBlock content = TextBlock.builder().text("reply").build();
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
            if (closeCount.incrementAndGet() == 1) {
                closeOrder.add(name);
            }
        }
    }
}
