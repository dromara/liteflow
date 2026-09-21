package com.yomahub.liteflow.agent.component;

import io.agentscope.harness.agent.HarnessAgent;
import com.yomahub.liteflow.agent.harness.component.HarnessAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.middleware.ModelRoutingMiddleware;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.runtime.SkillRepositoryRegistration;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.agent.testsupport.AgentTestContexts;
import com.yomahub.liteflow.property.agent.AgentConfig;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.config.ModelConfig;
import io.agentscope.core.message.ContentBlock;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.middleware.ModelCallInput;
import io.agentscope.core.model.ChatResponse;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.GenerateOptions;
import io.agentscope.core.model.Model;
import io.agentscope.core.model.ToolSchema;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.permission.PermissionMode;
import io.agentscope.core.skill.AgentSkill;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.skill.repository.AgentSkillRepositoryInfo;
import io.agentscope.core.state.InMemoryAgentStateStore;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentBuilderConfigurationTest {

    private static final String AGENT_NAMESPACE =
            "lf-aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";

    @Test
    void usesOneHundredIterationsWhenNeitherConfigurationNorComponentOverridesIt() {
        TestComponent component = new TestComponent(new CloseableModel("default", new ArrayList<>()));
        component.iterations = -1;

        try (HarnessAgentRuntime runtime = component.runtime(config())) {
            assertEquals(100, runtime.agent().getDelegate().getReactConfig().maxIters());
        }
    }

    @Test
    void usesConfiguredIterationLimitUnlessComponentOverridesIt() {
        AgentConfig config = config();
        config.setMaxIterations(27);
        TestComponent component = new TestComponent(new CloseableModel("default", new ArrayList<>()));
        component.iterations = -1;

        try (HarnessAgentRuntime runtime = component.runtime(config)) {
            assertEquals(27, runtime.agent().getDelegate().getReactConfig().maxIters());
        }

        TestComponent override = new TestComponent(new CloseableModel("override", new ArrayList<>()));
        override.iterations = 7;
        try (HarnessAgentRuntime runtime = override.runtime(config)) {
            assertEquals(7, runtime.agent().getDelegate().getReactConfig().maxIters());
        }
    }

    @Test
    void rejectsNonPositiveConfiguredIterationLimits() {
        for (int limit : List.of(0, -1)) {
            AgentConfig config = config();
            config.setMaxIterations(limit);
            TestComponent component = new TestComponent(new CloseableModel("default", new ArrayList<>()));
            component.iterations = -1;

            assertThrows(AgentConfigException.class, () -> component.runtime(config));
        }
    }

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

        HarnessAgentRuntime runtime = component.runtime(config());
        try {
            ReActAgent agent = runtime.agent().getDelegate();
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

        HarnessAgentRuntime runtime = component.runtime(config());
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
    void buildFailureRemainsPrimaryWhenOwnedModelThrowsTheSameInstanceDuringRollback() {
        RuntimeException buildFailure = new RuntimeException("build and close failure");
        CloseableModel model = new CloseableModel(
                "default", new ArrayList<>(), buildFailure);
        TestComponent component = new TestComponent(model);
        component.customizer = builder -> {
            throw buildFailure;
        };

        RuntimeException thrown = assertThrows(
                RuntimeException.class, () -> component.runtime(config()));

        assertSame(buildFailure, thrown);
        assertEquals(0, buildFailure.getSuppressed().length);
        assertEquals(1, model.closeCount.get());
    }

    @Test
    void preparationPreservesRepositoryHookBeforeNamespacedStateStoreValidation() {
        List<String> hooks = new ArrayList<>();
        TestComponent component = new TestComponent(
                new CloseableModel("default", new ArrayList<>()));
        component.hookOrder = hooks;

        assertThrows(IllegalArgumentException.class,
                () -> component.runtime(config(), "unsafe-namespace"));

        assertEquals(List.of("resolver", "repositories"), hooks);
    }

    @Test
    void invalidNamespaceRollsBackAlreadyCollectedOwnedRepository() {
        List<String> closeOrder = new ArrayList<>();
        RuntimeException cleanupFailure = new RuntimeException("repository cleanup failed");
        CloseableRepository repository =
                new CloseableRepository("repository", closeOrder, cleanupFailure);
        TestComponent component = new TestComponent(
                new CloseableModel("must-not-build", new ArrayList<>()));
        component.repositoryRegistrations =
                List.of(SkillRepositoryRegistration.owned(repository));

        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> component.runtime(config(), "unsafe-namespace"));

        assertEquals(1, repository.closeCount.get());
        assertEquals(List.of("repository"), closeOrder);
        assertEquals(1, failure.getSuppressed().length);
        assertSame(cleanupFailure, failure.getSuppressed()[0]);
    }

    @Test
    void repositoryCollectionFailureRollsBackAlreadyCollectedOwnedRepository() {
        List<String> closeOrder = new ArrayList<>();
        RuntimeException cleanupFailure = new RuntimeException("repository cleanup failed");
        CloseableRepository repository =
                new CloseableRepository("repository", closeOrder, cleanupFailure);
        TestComponent component = new TestComponent(
                new CloseableModel("must-not-build", new ArrayList<>()));
        component.repositoryRegistrations = Arrays.asList(
                SkillRepositoryRegistration.owned(repository), null);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(failure.getMessage().contains("skillRepositoryRegistrations"));
        assertEquals(1, repository.closeCount.get());
        assertEquals(List.of("repository"), closeOrder);
        assertEquals(1, failure.getSuppressed().length);
        assertSame(cleanupFailure, failure.getSuppressed()[0]);
    }

    @Test
    void nullRoutingEntryStillCollectsAndRollsBackLaterOwnedModels() {
        List<String> closeOrder = new ArrayList<>();
        CloseableModel defaultModel = new CloseableModel("default", closeOrder);
        CloseableModel routingOne = new CloseableModel("routing-1", closeOrder);
        CloseableModel routingTwo = new CloseableModel("routing-2", closeOrder);
        TestComponent component = new TestComponent(defaultModel);
        component.routing = Arrays.asList(routingOne, null, routingTwo);

        AgentConfigException failure = assertThrows(
                AgentConfigException.class, () -> component.runtime(config()));

        assertTrue(failure.getMessage().contains("routingModels"));
        assertEquals(1, defaultModel.closeCount.get());
        assertEquals(1, routingOne.closeCount.get());
        assertEquals(1, routingTwo.closeCount.get());
        assertEquals(List.of("routing-2", "routing-1", "default"), closeOrder);
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
    void sameBuilderMaySelectOtherManagedModelsAndRoutingUsesTheFinalPrimary() {
        CloseableModel originalDefault = new CloseableModel("original", new ArrayList<>());
        CloseableModel originalFallback = new CloseableModel("fallback", new ArrayList<>());
        CloseableModel finalPrimary = new CloseableModel("routing", new ArrayList<>());
        CloseableModel composedDefaultPath = new CloseableModel("composed", new ArrayList<>());
        TestComponent component = new TestComponent(originalDefault);
        component.fallback = originalFallback;
        component.routing = List.of(finalPrimary);
        component.customizer = builder -> builder
                .model(finalPrimary)
                .fallbackModel(originalDefault);

        HarnessAgentRuntime runtime = component.runtime(config());
        try {
            assertSame(finalPrimary, runtime.agent().getModel());
            assertSame(originalDefault, runtime.agent().getDelegate().getModelConfig().fallbackModel());

            ModelRoutingMiddleware routing = runtime.agent().getDelegate().getMiddlewares().stream()
                    .filter(ModelRoutingMiddleware.class::isInstance)
                    .map(ModelRoutingMiddleware.class::cast)
                    .findFirst()
                    .orElseThrow();
            AtomicReference<ModelCallInput> forwarded = new AtomicReference<>();
            ModelCallInput input = new ModelCallInput(
                    List.of(), List.of(), null, composedDefaultPath);
            routing.onModelCall(
                            runtime.agent(),
                            AgentTestContexts.runtimeContext(
                                    AgentTestContexts.liteFlowContext()),
                            input,
                            nextInput -> {
                                forwarded.set(nextInput);
                                return Flux.empty();
                            })
                    .blockLast();

            assertSame(composedDefaultPath, forwarded.get().model(),
                    "the final primary must preserve AgentScope's composed fallback model");
        } finally {
            runtime.close();
        }
    }

    @Test
    void distinctReturnedBuilderIsRejectedEvenWhenItRetainsCoreResources() {
        CloseableModel original = new CloseableModel("original", new ArrayList<>());
        TestComponent component = new TestComponent(original);
        component.customizer = builder -> copyBuilderRetainingLiteFlowResources(builder, original, null, true);
        AgentConfigException failure = assertThrows(AgentConfigException.class, () -> component.runtime(config()));
        assertTrue(failure.getMessage().contains("provided builder"));
        assertEquals(1, original.closeCount.get());
    }

    @Test
    void nullOrMissingMandatoryCustomizerResourcesAreRejectedAndOwnedModelsCloseOnce() {
        CloseableModel nullResultModel = new CloseableModel("null-result", new ArrayList<>());
        TestComponent nullResult = new TestComponent(nullResultModel);
        nullResult.customizer = builder -> null;

        AgentConfigException nullFailure = assertThrows(
                AgentConfigException.class, () -> nullResult.runtime(config()));
        assertTrue(nullFailure.getMessage().contains("must not return null"));
        assertEquals(1, nullResultModel.closeCount.get());

        CloseableModel missingStateModel = new CloseableModel("missing-state", new ArrayList<>());
        TestComponent missingState = new TestComponent(missingStateModel);
        missingState.customizer = builder -> HarnessAgent.builder()
                .name("unsafe")
                .sysPrompt("unsafe")
                .model(missingStateModel);

        AgentConfigException stateFailure = assertThrows(
                AgentConfigException.class, () -> missingState.runtime(config()));
        assertTrue(stateFailure.getMessage().contains("provided builder"));
        assertEquals(1, missingStateModel.closeCount.get());

        CloseableModel missingMiddlewareModel =
                new CloseableModel("missing-middleware", new ArrayList<>());
        TestComponent missingMiddleware = new TestComponent(missingMiddlewareModel);
        missingMiddleware.customizer = builder -> copyBuilderRetainingLiteFlowResources(
                builder, missingMiddlewareModel, null, false);

        AgentConfigException middlewareFailure = assertThrows(
                AgentConfigException.class, () -> missingMiddleware.runtime(config()));
        assertTrue(middlewareFailure.getMessage().contains("provided builder"));
        assertEquals(1, missingMiddlewareModel.closeCount.get());
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
        config.getHarness().getLocal().setWorkspaceRoot(java.nio.file.Path.of("target", "harness-tests", java.util.UUID.randomUUID().toString()).toAbsolutePath().toString());
        config.getSessionStore().setJsonWorkspaceRoot(config.getHarness().getLocal().getWorkspaceRoot() + "/records");
        config.getSessionStore().setJsonRoot("target/agent-state");
        config.setApplicationName("builder-test");
        return config;
    }

    private static HarnessAgent.Builder copyBuilderRetainingLiteFlowResources(
            HarnessAgent.Builder source,
            Model primary,
            Model fallback,
            boolean copyMiddlewares) {
        ReActAgent snapshot = source.build().getDelegate();
        try {
            HarnessAgent.Builder copy = HarnessAgent.Builder.fromAgent(snapshot)
                    .model(primary)
                    .fallbackModel(fallback)
                    .modelExecutionConfig(snapshot.getModelExecutionConfig())
                    .toolExecutionConfig(snapshot.getToolExecutionConfig())
                    .permissionContext(snapshot.getPermissionContext())
                    .stopOnReject(snapshot.getReactConfig().stopOnReject())
                    .defaultSessionId(snapshot.getDefaultSessionId())
                    .stateStore(snapshot.getStateStore());
            if (copyMiddlewares) {
                copy.middlewares(snapshot.getMiddlewares());
            }
            return copy;
        } finally {
            snapshot.close();
        }
    }

    private static final class TestComponent extends HarnessAgentComponent {
        // This fixture exercises non-Shell behavior; opt out of the enabled-by-default tool.
        @Override protected boolean enableShellTool() { return false; }
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
        private UnaryOperator<HarnessAgent.Builder> customizer = UnaryOperator.identity();
        private final AtomicInteger customizerCalls = new AtomicInteger();
        private List<String> hookOrder;
        private List<SkillRepositoryRegistration> repositoryRegistrations = List.of();

        private TestComponent(CloseableModel defaultModel) {
            this.defaultModel = defaultModel;
        }

        private HarnessAgentRuntime runtime(AgentConfig config) {
            return runtime(config, AGENT_NAMESPACE);
        }

        private HarnessAgentRuntime runtime(AgentConfig config, String namespace) {
            return buildRuntime(new AgentRuntimeBuildContext(
                    config, "builder-agent", "agent-key", namespace));
        }

        @Override
        protected ModelSpec<?> model() {
            throw new AssertionError("buildModel override must be used");
        }

        @Override
        protected Model buildModel() {
            if (hookOrder != null) {
                hookOrder.add("model");
            }
            return defaultModel;
        }

        @Override
        protected AgentStateStoreResolver stateStoreResolver() {
            if (hookOrder == null) {
                return super.stateStoreResolver();
            }
            return ignored -> {
                hookOrder.add("resolver");
                return new ResolvedAgentStateStore(new InMemoryAgentStateStore(), false);
            };
        }

        @Override
        protected List<SkillRepositoryRegistration> skillRepositoryRegistrations() {
            if (hookOrder != null) {
                hookOrder.add("repositories");
            }
            return repositoryRegistrations;
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
        protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
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

    private static final class CloseableRepository implements AgentSkillRepository {
        private final String name;
        private final List<String> closeOrder;
        private final RuntimeException closeFailure;
        private final AtomicInteger closeCount = new AtomicInteger();

        private CloseableRepository(
                String name, List<String> closeOrder, RuntimeException closeFailure) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.closeFailure = closeFailure;
        }

        @Override public AgentSkill getSkill(String name) { return null; }
        @Override public List<String> getAllSkillNames() { return List.of(); }
        @Override public List<AgentSkill> getAllSkills() { return List.of(); }
        @Override public boolean save(List<AgentSkill> skills, boolean force) { return false; }
        @Override public boolean delete(String skillName) { return false; }
        @Override public boolean skillExists(String skillName) { return false; }
        @Override public AgentSkillRepositoryInfo getRepositoryInfo() {
            return new AgentSkillRepositoryInfo("test", name, false);
        }
        @Override public String getSource() { return name; }
        @Override public void setWriteable(boolean writeable) { }
        @Override public boolean isWriteable() { return false; }

        @Override
        public void close() {
            closeCount.incrementAndGet();
            closeOrder.add(name);
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }

    private static final class CloseableModel implements Model, AutoCloseable {
        private final String name;
        private final List<String> closeOrder;
        private final RuntimeException closeFailure;
        private final AtomicInteger closeCount = new AtomicInteger();

        private CloseableModel(String name, List<String> closeOrder) {
            this(name, closeOrder, null);
        }

        private CloseableModel(
                String name, List<String> closeOrder, RuntimeException closeFailure) {
            this.name = name;
            this.closeOrder = closeOrder;
            this.closeFailure = closeFailure;
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
            if (closeFailure != null) {
                throw closeFailure;
            }
        }
    }
}
