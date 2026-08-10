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
import com.yomahub.liteflow.agent.runtime.McpClientRegistration;
import com.yomahub.liteflow.agent.runtime.ReActAgentRuntime;
import com.yomahub.liteflow.agent.state.AgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.DefaultAgentStateStoreResolver;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.agent.tool.GuardedWorkspacePathResolver;
import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.agent.tool.WorkspaceFileTools;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import com.yomahub.liteflow.property.agent.WorkspaceBackend;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.agent.config.ModelConfig;
import io.agentscope.core.message.Msg;
import io.agentscope.core.model.ExecutionConfig;
import io.agentscope.core.model.Model;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionContextState;
import io.agentscope.core.skill.DynamicSkillMiddleware;
import io.agentscope.core.skill.SkillFilter;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.tool.AgentTool;
import io.agentscope.core.tool.Toolkit;
import io.agentscope.core.tool.ToolkitConfig;
import io.agentscope.core.tool.mcp.McpClientWrapper;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    protected List<Object> tools() {
        return List.of();
    }

    protected void customizeToolkit(Toolkit toolkit) {
    }

    protected List<McpClientWrapper> mcpClients() {
        return List.of();
    }

    protected boolean ownsMcpClient(McpClientWrapper client) {
        return false;
    }

    protected List<AgentSkillRepository> skillRepositories() {
        return List.of();
    }

    protected boolean ownsSkillRepository(AgentSkillRepository repository) {
        return false;
    }

    protected SkillFilter skillFilter() {
        return SkillFilter.all();
    }

    protected boolean dynamicSkillsEnabled() {
        return true;
    }

    protected boolean enableWorkspaceFileTools() {
        return false;
    }

    protected boolean enableShellTool() {
        return false;
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
        List<McpClientRegistration> registeredMcpClients = new ArrayList<>();
        List<AgentSkillRepository> repositories = new ArrayList<>();
        List<AgentSkillRepository> ownedRepositories = new ArrayList<>();
        ReActAgent agent = null;
        try {
            collectSkillRepositories(repositories, ownedRepositories);
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
            SkillFilter baseSkillFilter = skillFilter();
            if (baseSkillFilter == null) {
                throw new AgentConfigException("skillFilter must not return null");
            }
            boolean dynamicSkills = dynamicSkillsEnabled();
            Toolkit toolkit = buildToolkit(buildContext.agentConfig(), registeredMcpClients);
            DynamicSkillMiddleware managedDynamicSkills =
                    !repositories.isEmpty() && dynamicSkills
                            ? new DynamicSkillMiddleware(
                                    repositories, toolkit, baseSkillFilter, false, null)
                            : null;
            List<MiddlewareBase> mandatoryMiddlewares = new ArrayList<>(List.of(
                    failureMiddleware,
                    loggingMiddleware,
                    eventMiddleware,
                    usageMiddleware,
                    skillMiddleware,
                    promptMiddleware,
                    routingMiddleware));
            if (managedDynamicSkills != null) {
                mandatoryMiddlewares.add(managedDynamicSkills);
            }
            Map<String, AgentTool> requiredTools = registeredToolIdentities(toolkit);
            ReActAgent.Builder builder = ReActAgent.builder()
                    .name(buildContext.agentName())
                    .sysPrompt(effectiveSystemPrompt())
                    .model(defaultModel)
                    .toolkit(toolkit)
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
            if (managedDynamicSkills != null) {
                builder.middleware(managedDynamicSkills);
            }
            for (AgentSkillRepository repository : repositories) {
                builder.skillRepository(repository);
            }
            builder.skillFilter(baseSkillFilter)
                    .dynamicSkillsEnabled(false)
                    .skillCodeExecutionEnabled(false);
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
                    agent,
                    namespaced,
                    managedModels,
                    mandatoryMiddlewares,
                    requiredTools,
                    managedDynamicSkills);
            routingMiddleware.finalizeDefaultModel(agent.getModel());
            return new ReActAgentRuntime(
                    agent,
                    namespaced,
                    resolved,
                    registeredMcpClients,
                    ownedRepositories,
                    managedModels);
        } catch (RuntimeException | Error failure) {
            closeAfterBuildFailure(
                    failure,
                    agent,
                    registeredMcpClients,
                    ownedRepositories,
                    ownedModels,
                    namespaced,
                    resolved);
            throw failure;
        }
    }

    @Override
    protected boolean requiresWorkspaceLease() {
        return enableWorkspaceFileTools() || enableShellTool();
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
            List<MiddlewareBase> mandatoryMiddlewares,
            Map<String, AgentTool> requiredTools,
            DynamicSkillMiddleware managedDynamicSkills) {
        if (agent.getStateStore() != namespacedStateStore) {
            throw new AgentConfigException(
                    "customizeAgent must retain the LiteFlow namespaced StateStore");
        }
        for (MiddlewareBase mandatory : mandatoryMiddlewares) {
            if (!identityContains(agent.getMiddlewares(), mandatory)) {
                if (mandatory == managedDynamicSkills) {
                    throw new AgentConfigException(
                            "customizeAgent must retain the managed DynamicSkillMiddleware");
                }
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
        for (Map.Entry<String, AgentTool> required : requiredTools.entrySet()) {
            if (agent.getToolkit().getTool(required.getKey()) != required.getValue()) {
                throw new AgentConfigException(
                        "customizeAgent must retain the LiteFlow Toolkit tool identity: "
                                + required.getKey());
            }
        }
        if (managedDynamicSkills != null) {
            for (MiddlewareBase middleware : agent.getMiddlewares()) {
                if (middleware instanceof DynamicSkillMiddleware
                        && middleware != managedDynamicSkills) {
                    throw new AgentConfigException(
                            "customizeAgent must not add or replace the managed "
                                    + "DynamicSkillMiddleware");
                }
            }
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
            List<McpClientRegistration> mcpClients,
            List<AgentSkillRepository> repositories,
            List<Model> models,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore) {
        addCloseFailure(failure, agent);
        Set<McpClientWrapper> closedMcp =
                Collections.newSetFromMap(new IdentityHashMap<>());
        for (int index = mcpClients.size() - 1; index >= 0; index--) {
            McpClientRegistration registration = mcpClients.get(index);
            if (registration.owned() && closedMcp.add(registration.client())) {
                addCloseFailure(failure, registration.client());
            }
        }
        Set<AgentSkillRepository> closedRepositories =
                Collections.newSetFromMap(new IdentityHashMap<>());
        for (int index = repositories.size() - 1; index >= 0; index--) {
            AgentSkillRepository repository = repositories.get(index);
            if (closedRepositories.add(repository)) {
                addCloseFailure(failure, repository);
            }
        }
        for (int index = models.size() - 1; index >= 0; index--) {
            addCloseFailure(failure, models.get(index));
        }
        addCloseFailure(failure, stateStore);
        addCloseFailure(failure, resolvedStateStore);
    }

    private Toolkit buildToolkit(
            AgentConfig config, List<McpClientRegistration> registeredMcpClients) {
        if (config.getToolkit() == null) {
            throw new AgentConfigException("liteflow.agent.toolkit must not be null");
        }
        Toolkit toolkit = new Toolkit(ToolkitConfig.builder()
                .parallel(config.getToolkit().isParallel())
                .build());
        List<Object> configuredTools = tools();
        if (configuredTools == null) {
            throw new AgentConfigException("tools must not return null");
        }
        for (Object tool : configuredTools) {
            if (tool == null) {
                throw new AgentConfigException("tools must not contain null");
            }
            toolkit.registerTool(tool);
        }

        if (enableWorkspaceFileTools() || enableShellTool()) {
            GuardedWorkspacePathResolver workspace = guardedWorkspace(config);
            if (enableWorkspaceFileTools()) {
                toolkit.registerTool(new WorkspaceFileTools(workspace, config));
            }
            if (enableShellTool()) {
                if (config.getShell() == null || config.getShell().getMode() == null
                        || config.getShell().getMode() == ShellMode.DISABLED) {
                    throw new AgentConfigException(
                            "enableShellTool requires liteflow.agent.shell.mode != DISABLED");
                }
                toolkit.registerTool(new ManagedShellCommandTool(workspace, config));
            }
        }

        customizeToolkit(toolkit);
        List<McpClientWrapper> clients = mcpClients();
        if (clients == null) {
            throw new AgentConfigException("mcpClients must not return null");
        }
        Set<McpClientWrapper> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (McpClientWrapper client : clients) {
            if (client == null) {
                throw new AgentConfigException("mcpClients must not contain null");
            }
            if (!seen.add(client)) {
                continue;
            }
            McpClientRegistration registration =
                    new McpClientRegistration(client, ownsMcpClient(client));
            registeredMcpClients.add(registration);
            toolkit.registerMcpClient(client)
                    .timeout(config.getRuntime().getTimeout())
                    .block();
        }
        return toolkit;
    }

    private GuardedWorkspacePathResolver guardedWorkspace(AgentConfig config) {
        if (config.getWorkspace() == null) {
            throw new AgentConfigException("liteflow.agent.workspace must not be null");
        }
        if (config.getWorkspace().getBackend() != WorkspaceBackend.GUARDED_LOCAL) {
            throw new AgentConfigException(
                    "built-in workspace tools require WorkspaceBackend.GUARDED_LOCAL");
        }
        if (!config.getWorkspace().isTrustedLocal()) {
            throw new AgentConfigException(
                    "built-in workspace tools require workspace.trustedLocal=true");
        }
        String root = config.getWorkspace().getRoot();
        if (root == null || root.isBlank()) {
            throw new AgentConfigException(
                    "built-in workspace tools require a valid workspace.root");
        }
        if (config.getWorkspace().getMaxFileBytes() <= 0) {
            throw new AgentConfigException("workspace.maxFileBytes must be positive");
        }
        if (config.getWorkspace().getMaxListSize() <= 0) {
            throw new AgentConfigException("workspace.maxListSize must be positive");
        }
        try {
            return new GuardedWorkspacePathResolver(
                    Path.of(root),
                    config.getWorkspace().getMaxFileBytes(),
                    config.getWorkspace().isAutoCreate());
        } catch (IllegalArgumentException failure) {
            throw new AgentConfigException("invalid guarded local workspace.root", failure);
        }
    }

    private void collectSkillRepositories(
            List<AgentSkillRepository> repositories,
            List<AgentSkillRepository> ownedRepositories) {
        List<AgentSkillRepository> configured = skillRepositories();
        if (configured == null) {
            throw new AgentConfigException("skillRepositories must not return null");
        }
        Set<AgentSkillRepository> seen =
                Collections.newSetFromMap(new IdentityHashMap<>());
        for (AgentSkillRepository repository : configured) {
            if (repository == null) {
                throw new AgentConfigException("skillRepositories must not contain null");
            }
            if (!seen.add(repository)) {
                continue;
            }
            repositories.add(repository);
            if (ownsSkillRepository(repository)) {
                ownedRepositories.add(repository);
            }
        }
    }

    private static Map<String, AgentTool> registeredToolIdentities(Toolkit toolkit) {
        Map<String, AgentTool> tools = new LinkedHashMap<>();
        for (String name : toolkit.getToolNames()) {
            tools.put(name, toolkit.getTool(name));
        }
        return Map.copyOf(tools);
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
