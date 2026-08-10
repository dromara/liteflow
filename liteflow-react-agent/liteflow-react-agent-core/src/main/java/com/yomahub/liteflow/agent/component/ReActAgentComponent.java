package com.yomahub.liteflow.agent.component;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.middleware.AgentMiddlewareOrder;
import com.yomahub.liteflow.agent.middleware.ChatUsageMiddleware;
import com.yomahub.liteflow.agent.middleware.FlowEventBridgeMiddleware;
import com.yomahub.liteflow.agent.middleware.LiteFlowSystemPromptMiddleware;
import com.yomahub.liteflow.agent.middleware.ModelRoutingMiddleware;
import com.yomahub.liteflow.agent.middleware.ReActLoggingMiddleware;
import com.yomahub.liteflow.agent.middleware.SkillTrackingMiddleware;
import com.yomahub.liteflow.agent.middleware.StateStoreFailureMiddleware;
import com.yomahub.liteflow.agent.model.ModelSpec;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agent.config.ModelConfig;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.tool.Toolkit;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** AgentScope 2 ReAct specialization of the shared LiteFlow invocation template. */
public abstract class ReActAgentComponent extends AbstractAgentComponent<ReActAgentRuntime> {

    public static final String DEFAULT_SYSTEM_PROMPT = """
            请使用用户提问所用的语言回答，除非用户明确要求使用其他语言。
            每次调用工具前，先用一两句话简短说明当前判断和下一步动作，便于日志观察可见推理摘要。
            不要展开隐藏思维链，只输出面向用户和调试日志都可读的简短说明。
            """;

    protected abstract ModelSpec<?> model();

    protected Model buildModel() {
        return model().resolve(agentConfig());
    }

    protected final String effectiveSystemPrompt() {
        String customPrompt = systemPrompt();
        if (customPrompt == null || customPrompt.isBlank()) {
            return DEFAULT_SYSTEM_PROMPT.strip();
        }
        return DEFAULT_SYSTEM_PROMPT.strip() + "\n\n" + customPrompt.strip();
    }

    protected int maxIterations() {
        return -1;
    }

    protected ExecutionConfig modelExecutionConfig() {
        return null;
    }

    protected ExecutionConfig toolExecutionConfig() {
        return null;
    }

    protected int maxRetries() {
        return ModelConfig.DEFAULT_MAX_RETRIES;
    }

    protected Model fallbackModel() {
        return null;
    }

    protected List<Model> routingModels() {
        return List.of();
    }

    protected PermissionContextState permissionContext() {
        return null;
    }

    protected boolean stopOnReject() {
        return false;
    }

    protected List<MiddlewareBase> middlewares() {
        return List.of();
    }

    protected Model routeModel(Model defaultModel, LiteFlowAgentContext context) {
        return defaultModel;
    }

    protected ReActAgent.Builder customizeAgent(ReActAgent.Builder builder) {
        return builder;
    }

    protected AgentStateStoreResolver stateStoreResolver() {
        return new DefaultAgentStateStoreResolver();
    }

    @Override
    protected ReActAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
        BuildOptions options = buildOptions(buildContext);
        AgentStateStoreResolver resolver = stateStoreResolver();
        if (resolver == null) {
            throw new AgentConfigException("stateStoreResolver must not return null");
        }
        ResolvedAgentStateStore resolved = resolver.resolve(
                buildContext.agentConfig().getStateStore());
        if (resolved == null) {
            throw new AgentConfigException("AgentStateStoreResolver.resolve must not return null");
        }
        GuardedNamespacedAgentStateStore namespaced = null;
        List<Model> ownedModels = new ArrayList<>();
        ReActAgent agent = null;
        try {
            namespaced = new GuardedNamespacedAgentStateStore(
                    resolved.store(), buildContext.agentNamespace());
            Model defaultModel = requireModel(buildModel(), "buildModel must not return null");
            addIdentityDistinct(ownedModels, defaultModel);
            Model fallback = fallbackModel();
            if (fallback != null) {
                addIdentityDistinct(ownedModels, fallback);
            }
            List<Model> routing = routingModels();
            if (routing == null) {
                throw new AgentConfigException("routingModels must not return null");
            }
            boolean nullRoute = false;
            for (Model candidate : routing) {
                if (candidate == null) {
                    nullRoute = true;
                } else {
                    addIdentityDistinct(ownedModels, candidate);
                }
            }
            if (nullRoute) {
                throw new AgentConfigException("routingModels must not contain null");
            }

            StateStoreFailureMiddleware failureMiddleware = new StateStoreFailureMiddleware(
                    namespaced,
                    buildContext.agentConfig().getStateStore().getFailurePolicy());
            List<Model> managedModels = List.copyOf(ownedModels);
            ReActLoggingMiddleware loggingMiddleware =
                    new ReActLoggingMiddleware(options.loggingEnabled());
            FlowEventBridgeMiddleware eventMiddleware =
                    new FlowEventBridgeMiddleware(options.listenerFailureMode());
            ChatUsageMiddleware usageMiddleware = new ChatUsageMiddleware();
            SkillTrackingMiddleware skillMiddleware = new SkillTrackingMiddleware(Map.of());
            LiteFlowSystemPromptMiddleware promptMiddleware =
                    new LiteFlowSystemPromptMiddleware(this::transformSystemPrompt);
            ModelRoutingMiddleware routingMiddleware =
                    ModelRoutingMiddleware.awaitingDefaultModel(
                            managedModels, this::routeModel);
            List<MiddlewareBase> mandatoryMiddlewares = List.of(
                    failureMiddleware,
                    loggingMiddleware,
                    eventMiddleware,
                    usageMiddleware,
                    skillMiddleware,
                    promptMiddleware,
                    routingMiddleware);
            ReActAgent.Builder builder = ReActAgent.builder()
                    .name(buildContext.agentName())
                    .sysPrompt(effectiveSystemPrompt())
                    .model(defaultModel)
                    .toolkit(new Toolkit())
                    .maxIters(options.maxIterations())
                    .modelExecutionConfig(options.modelExecutionConfig())
                    .toolExecutionConfig(options.toolExecutionConfig())
                    .maxRetries(options.maxRetries())
                    .fallbackModel(fallback)
                    .permissionContext(options.permissionContext())
                    .stopOnReject(options.stopOnReject())
                    .defaultSessionId(buildContext.agentNamespace())
                    .stateStore(namespaced)
                    .middleware(failureMiddleware)
                    .middleware(loggingMiddleware)
                    .middleware(eventMiddleware)
                    .middleware(usageMiddleware)
                    .middleware(skillMiddleware);
            for (MiddlewareBase middleware : options.userMiddlewares()) {
                builder.middleware(AgentMiddlewareOrder.user(middleware));
            }
            builder.middleware(promptMiddleware);
            builder.middleware(routingMiddleware);

            ReActAgent.Builder customized = customizeAgent(builder);
            if (customized == null) {
                throw new AgentConfigException("customizeAgent must not return null");
            }
            agent = customized.build();
            validateCustomizedAgent(
                    agent, namespaced, managedModels, mandatoryMiddlewares);
            routingMiddleware.finalizeDefaultModel(agent.getModel());
            return new ReActAgentRuntime(agent, namespaced, resolved, managedModels);
        } catch (RuntimeException | Error failure) {
            closeAfterBuildFailure(failure, agent, ownedModels, namespaced, resolved);
            throw failure;
        }
    }

    @Override
    protected Mono<Msg> invokeRuntime(
            ReActAgentRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext) {
        if (!runtime.stateStore().agentNamespace().equals(liteflowContext.getAgentNamespace())) {
            return Mono.error(new AgentConfigException(
                    "Agent identity changed after this component runtime was initialized"));
        }
        return switch (output.kind()) {
            case TEXT -> runtime.agent().call(input, runtimeContext);
            case JAVA_TYPE -> runtime.agent().call(input, output.javaType(), runtimeContext);
            case JSON_SCHEMA -> runtime.agent().call(input, output.jsonSchema(), runtimeContext);
        };
    }

    private BuildOptions buildOptions(AgentRuntimeBuildContext context) {
        int configuredIterations = maxIterations();
        int iterations;
        if (configuredIterations == -1) {
            if (context.agentConfig().getDefaults() == null) {
                throw new AgentConfigException("liteflow.agent.defaults must not be null");
            }
            iterations = context.agentConfig().getDefaults().getMaxIterations();
        } else {
            iterations = configuredIterations;
        }
        if (iterations <= 0) {
            throw new AgentConfigException("maxIterations must be positive");
        }

        int retries = maxRetries();
        if (retries <= 0) {
            throw new AgentConfigException("maxRetries must be positive");
        }
        List<MiddlewareBase> configuredMiddlewares = middlewares();
        if (configuredMiddlewares == null) {
            throw new AgentConfigException("middlewares must not return null");
        }
        List<MiddlewareBase> userMiddlewares = new ArrayList<>();
        for (MiddlewareBase middleware : configuredMiddlewares) {
            if (middleware == null) {
                throw new AgentConfigException("middlewares must not contain null");
            }
            userMiddlewares.add(middleware);
        }
        if (context.agentConfig().getEvent() == null
                || context.agentConfig().getEvent().getListenerFailureMode() == null) {
            throw new AgentConfigException(
                    "liteflow.agent.event.listener-failure-mode must not be null");
        }
        if (context.agentConfig().getLogging() == null) {
            throw new AgentConfigException("liteflow.agent.logging must not be null");
        }
        return new BuildOptions(
                iterations,
                modelExecutionConfig(),
                toolExecutionConfig(),
                retries,
                permissionContext(),
                stopOnReject(),
                List.copyOf(userMiddlewares),
                context.agentConfig().getEvent().getListenerFailureMode(),
                context.agentConfig().getLogging().isReactEnabled());
    }

    private static Model requireModel(Model model, String message) {
        if (model == null) {
            throw new AgentConfigException(message);
        }
        return model;
    }

    private static void validateCustomizedAgent(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore namespacedStateStore,
            List<Model> managedModels,
            List<MiddlewareBase> mandatoryMiddlewares) {
        if (agent.getStateStore() != namespacedStateStore) {
            throw new AgentConfigException(
                    "customizeAgent must retain the LiteFlow namespaced StateStore");
        }
        for (MiddlewareBase mandatory : mandatoryMiddlewares) {
            if (!identityContains(agent.getMiddlewares(), mandatory)) {
                throw new AgentConfigException(
                        "customizeAgent must retain all LiteFlow core middlewares");
            }
        }
        if (!identityContains(managedModels, agent.getModel())) {
            throw new AgentConfigException(
                    "customizeAgent introduced an unmanaged primary model");
        }
        Model customizedFallback = agent.getModelConfig().fallbackModel();
        if (customizedFallback != null && !identityContains(managedModels, customizedFallback)) {
            throw new AgentConfigException(
                    "customizeAgent introduced an unmanaged fallback model");
        }
    }

    private static boolean identityContains(
            List<? extends MiddlewareBase> middlewares, MiddlewareBase expected) {
        for (MiddlewareBase middleware : middlewares) {
            if (middleware == expected) {
                return true;
            }
        }
        return false;
    }

    private static boolean identityContains(List<Model> models, Model expected) {
        for (Model model : models) {
            if (model == expected) {
                return true;
            }
        }
        return false;
    }

    private static void addIdentityDistinct(List<Model> models, Model candidate) {
        if (!identityContains(models, candidate)) {
            models.add(candidate);
        }
    }

    private static void closeAfterBuildFailure(
            Throwable failure,
            ReActAgent agent,
            List<Model> models,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore) {
        addCloseFailure(failure, agent);
        for (int index = models.size() - 1; index >= 0; index--) {
            addCloseFailure(failure, models.get(index));
        }
        addCloseFailure(failure, stateStore);
        addCloseFailure(failure, resolvedStateStore);
    }

    private static void addCloseFailure(Throwable failure, Object resource) {
        if (!(resource instanceof AutoCloseable closeable)) {
            return;
        }
        try {
            closeable.close();
        } catch (Throwable closeFailure) {
            failure.addSuppressed(closeFailure);
        }
    }

    private record BuildOptions(
            int maxIterations,
            ExecutionConfig modelExecutionConfig,
            ExecutionConfig toolExecutionConfig,
            int maxRetries,
            PermissionContextState permissionContext,
            boolean stopOnReject,
            List<MiddlewareBase> userMiddlewares,
            com.yomahub.liteflow.property.agent.AgentListenerFailureMode listenerFailureMode,
            boolean loggingEnabled) {
    }
}
