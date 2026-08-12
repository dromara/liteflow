package com.yomahub.liteflow.agent.harness.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.component.AbstractReActLikeAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.GuardedLocalFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.harness.runtime.SandboxCallGate;
import com.yomahub.liteflow.agent.harness.sandbox.DockerSandboxConfigurer;
import com.yomahub.liteflow.agent.harness.sandbox.SandboxSnapshotProvider;
import com.yomahub.liteflow.agent.harness.state.HarnessNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.hitl.AgentCallTarget;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.middleware.AgentMiddlewareOrder;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.PreparedAgentResources;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.state.AgentStateStore;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.WorkspaceTaskRepository;
import io.agentscope.harness.agent.sandbox.SandboxContext;
import io.agentscope.harness.agent.sandbox.SandboxClient;
import io.agentscope.harness.agent.sandbox.impl.docker.DockerSandboxClientOptions;
import io.agentscope.harness.agent.workspace.WorkspaceManager;
import io.agentscope.harness.agent.workspace.WorkspacePathNormalizer;
import reactor.core.publisher.Mono;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/** AgentScope Harness specialization using the shared LiteFlow component lifecycle. */
public abstract class HarnessAgentComponent
        extends AbstractReActLikeAgentComponent<HarnessAgentRuntime> {

    private volatile Runnable filesystemBeforeCall = () -> { };

    protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
        return builder;
    }

    protected CompactionConfig compactionConfig() {
        return null;
    }

    protected MemoryConfig memoryConfig() {
        return null;
    }

    /** Additional workspace-relative files loaded into the Harness system context, in order. */
    protected List<String> additionalContextFiles() {
        return List.of();
    }

    protected List<SubagentDeclaration> subagents() {
        return List.of();
    }

    protected boolean enablePlanMode() {
        return false;
    }

    /** Explicit extension point used only by the CUSTOM filesystem backend. */
    protected HarnessFilesystemConfigurer filesystemConfigurer() {
        return null;
    }

    protected SandboxSnapshotProvider sandboxSnapshotProvider() {
        return null;
    }

    /**
     * Trusted Java custom-backend seam for Docker sandboxes; {@code null} uses the default client.
     * LiteFlow still owns the filesystem spec, Docker options, projection policy, and identity.
     */
    protected SandboxClient<DockerSandboxClientOptions> dockerSandboxClient() {
        return null;
    }

    protected TaskRepository taskRepository() {
        return null;
    }

    protected boolean ownsTaskRepository(TaskRepository repository) {
        return false;
    }

    protected ToolResultEvictionConfig toolResultEvictionConfig() {
        return null;
    }

    /** Additional provider resources owned by this component, in build order. */
    protected List<? extends AutoCloseable> ownedHarnessResources() {
        return List.of();
    }

    @Override
    protected final boolean requiresWorkspaceLease() {
        return true;
    }

    @Override
    protected final HarnessNamespacedAgentStateStore createNamespacedStateStore(
            AgentStateStore delegate, String agentNamespace) {
        return new HarnessNamespacedAgentStateStore(delegate, agentNamespace);
    }

    @Override
    protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
        FilesystemPreparation filesystem = validateAndPrepareFilesystem(
                buildContext.agentConfig(), buildContext.agentNamespace());
        PreparedAgentResources prepared = prepareAgentResources(buildContext, false, true);
        HarnessAgent agent = null;
        List<AutoCloseable> ownedProviderResources = new ArrayList<>();
        AutoCloseable workspaceTaskRollback = null;
        try {
            collectOwnedHarnessResources(ownedProviderResources);
            TaskRepository tasks = taskRepository();
            boolean ownsTasks = tasks != null && ownsTaskRepository(tasks);
            if (tasks instanceof WorkspaceTaskRepository workspaceTasks) {
                if (!ownsTasks) {
                    throw new AgentConfigException(
                            "borrowed WorkspaceTaskRepository is unsupported because "
                                    + "HarnessAgent assumes shutdown ownership");
                }
                workspaceTaskRollback = workspaceTasks::shutdown;
                addIdentityDistinct(ownedProviderResources, workspaceTaskRollback);
            }
            else if (ownsTasks) {
                if (!(tasks instanceof AutoCloseable closeable)) {
                    throw new AgentConfigException(
                            "owned TaskRepository must implement AutoCloseable");
                }
                addIdentityDistinct(ownedProviderResources, closeable);
            }

            HarnessAgent.Builder builder = HarnessAgent.builder()
                    .name(buildContext.agentName())
                    .agentId(buildContext.agentNamespace())
                    .model(prepared.defaultModel())
                    .sysPrompt(effectiveSystemPrompt())
                    .toolkit(prepared.toolkit())
                    .maxIters(prepared.maxIterations())
                    .modelExecutionConfig(prepared.modelExecutionConfig())
                    .toolExecutionConfig(prepared.toolExecutionConfig())
                    .maxRetries(prepared.maxRetries())
                    .fallbackModel(prepared.fallbackModel())
                    .stopOnReject(prepared.stopOnReject())
                    .defaultSessionId(buildContext.agentNamespace())
                    .stateStore(prepared.ownership().stateStore())
                    .workspace(filesystem.context().workspaceRoot());
            filesystem.configurer().configure(builder, filesystem.context());
            boolean guardedLocal = buildContext.agentConfig().getHarness().getFilesystemBackend()
                    == HarnessFilesystemBackend.GUARDED_LOCAL;
            if (guardedLocal) {
                // Task 9 may reopen subagents only with an explicitly guarded child factory.
                builder.disableSubagents();
            }
            HarnessAgentBuilderFilesystemBridge.FilesystemSnapshot filesystemSnapshot =
                    HarnessAgentBuilderFilesystemBridge.snapshot(builder);
            HarnessAgentBuilderFilesystemBridge.ToolkitSnapshot toolkitSnapshot =
                    HarnessAgentBuilderFilesystemBridge.snapshotToolkit(
                            builder, prepared.toolkit());
            CompactionConfig compaction = compactionConfig();
            if (compaction != null) {
                builder.compaction(compaction);
            }
            MemoryConfig memory = memoryConfig();
            if (memory != null) {
                builder.memory(memory);
            }
            for (String contextFile : requireAdditionalContextFiles()) {
                builder.additionalContextFile(contextFile);
            }
            for (var repository : prepared.skillRepositories()) {
                builder.skillRepository(repository);
            }
            builder.skillFilter(prepared.skillFilter());
            if (!prepared.dynamicSkillsEnabled()) {
                builder.disableDynamicSkills();
            }
            List<SubagentDeclaration> declarations = requireSubagents();
            for (SubagentDeclaration declaration : declarations) {
                builder.subagent(declaration);
            }
            if (tasks != null) {
                builder.taskRepository(tasks);
            }
            var permissionContext =
                    HarnessAgentBuilderPermissionBridge.failClosed(prepared.permissionContext());
            boolean planMode = enablePlanMode();
            builder.enableTaskList()
                    .enablePlanMode(planMode)
                    .permissionContext(permissionContext);
            ToolResultEvictionConfig eviction = toolResultEvictionConfig();
            if (eviction != null) {
                builder.toolResultEviction(eviction);
            }
            addLiteFlowMiddlewares(builder, prepared);
            HarnessAgentBuilderCapabilitiesBridge.CapabilitiesSnapshot capabilitySnapshot =
                    HarnessAgentBuilderCapabilitiesBridge.snapshot(builder);
            HarnessAgentBuilderPermissionBridge.PermissionSnapshot permissionSnapshot =
                    HarnessAgentBuilderPermissionBridge.snapshot(builder, permissionContext);

            HarnessAgent.Builder customized = customizeHarness(builder);
            if (customized == null) {
                throw new AgentConfigException("customizeHarness must not return null");
            }
            if (customized != builder) {
                throw new AgentConfigException(
                        "customizeHarness must mutate and return the provided builder");
            }
            filesystemSnapshot.requireUnchanged(customized);
            toolkitSnapshot.requireUnchanged(customized);
            permissionSnapshot.requireUnchanged(customized);
            capabilitySnapshot.requireUnchanged(
                    customized,
                    compaction != null,
                    memory != null,
                    eviction != null,
                    planMode);
            HarnessAgentBuilderSubagentPermissionBridge.DynamicRefreshGuard subagentPermissions =
                    HarnessAgentBuilderSubagentPermissionBridge.dynamicRefreshGuard(
                            permissionContext);
            customized.middleware(subagentPermissions);
            HarnessAgentBuilderPermissionBridge.MiddlewareSnapshot permissionMiddleware =
                    HarnessAgentBuilderPermissionBridge.installInnermostGuard(
                            customized,
                            HarnessRuntimeContextContinuation.captureMiddleware(permissionContext));
            permissionMiddleware.requireUnchanged(customized);
            if (guardedLocal) {
                HarnessAgentBuilderFilesystemBridge.requireGuardedLocalSubagentsSafe(
                        customized, prepared.toolkit());
            }
            HarnessAgentBuilderTaskOwnershipBridge.TaskOwnershipSnapshot taskOwnership =
                    HarnessAgentBuilderTaskOwnershipBridge.snapshot(customized, tasks);
            HarnessAgentBuilderFilesystemBridge.preflightKnownBuildFailures(
                    customized, filesystemSnapshot);
            agent = customized.build();
            permissionMiddleware.requireFinal(agent);
            subagentPermissions.bind(agent);
            HarnessAgentBuilderSubagentPermissionBridge.inheritDeclaredLocalPermissions(
                    agent, permissionContext);
            if (guardedLocal) {
                HarnessAgentBuilderFilesystemBridge.requireGuardedLocalToolkitSafe(
                        agent.getToolkit());
            }
            if (workspaceTaskRollback != null
                    && taskOwnership.harnessWillShutdownWorkspaceTasks()) {
                ownedProviderResources.remove(workspaceTaskRollback);
            }
            validateCustomizedRuntime(
                    "customizeHarness",
                    agent.getStateStore(),
                    agent.getDelegate().getMiddlewares(),
                    agent.getModel(),
                    agent.getDelegate().getModelConfig().fallbackModel(),
                    agent.getDelegate().getToolkit(),
                    prepared);
            prepared.routingMiddleware().finalizeDefaultModel(agent.getModel());
            filesystemBeforeCall = filesystem.beforeCall();
            SandboxCallGate sandboxCallGate = filesystem.dockerBackend()
                    ? new SandboxCallGate()
                    : null;
            return new HarnessAgentRuntime(
                    agent, prepared.ownership(), ownedProviderResources, sandboxCallGate);
        }
        catch (RuntimeException | Error failure) {
            prepared.ownership().rollback(failure, agent, ownedProviderResources);
            throw failure;
        }
    }

    @Override
    protected Mono<Msg> invokeRuntime(
            HarnessAgentRuntime runtime,
            List<Msg> input,
            AgentOutputSpec output,
            RuntimeContext runtimeContext,
            LiteFlowAgentContext liteflowContext) {
        HarnessAgent agent = runtime.agent();
        requireNoReservedRuntimeValues(runtimeContext);
        HarnessRuntimeContextContinuation continuation =
                HarnessRuntimeContextContinuation.create();
        runtimeContext.put(HarnessRuntimeContextContinuation.class, continuation);
        Mono<Msg> invocation = runtime.executeSandboxCall(() -> invokeCallTarget(
                new AgentCallTarget() {
                    @Override
                    public Mono<Msg> call(List<Msg> messages, RuntimeContext context) {
                        return invokeHarnessPublicCall(
                                () -> agent.call(messages, continuation.effectiveOr(context)));
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, Class<?> type, RuntimeContext context) {
                        return invokeHarnessPublicCall(
                                () -> agent.call(
                                        messages, type, continuation.effectiveOr(context)));
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, JsonNode schema, RuntimeContext context) {
                        return invokeHarnessPublicCall(
                                () -> agent.call(
                                        messages, schema, continuation.effectiveOr(context)));
                    }
                },
                runtime.stateStore(),
                input,
                output,
                runtimeContext,
                liteflowContext));
        return Mono.using(
                () -> continuation,
                ignored -> clearStateLoadFailureOnTermination(
                        invocation, runtime.stateStore(), runtimeContext),
                ignored -> runtimeContext.put(HarnessRuntimeContextContinuation.class, null),
                true);
    }

    static <T> Mono<T> clearStateLoadFailureOnTermination(
            Mono<T> invocation,
            GuardedNamespacedAgentStateStore stateStore,
            RuntimeContext runtimeContext) {
        return Mono.using(
                () -> stateStore,
                ignored -> invocation,
                ignored -> stateStore.clearLoadFailure(
                        runtimeContext.getUserId(), runtimeContext.getSessionId()),
                true);
    }

    private Mono<Msg> invokeHarnessPublicCall(Supplier<Mono<Msg>> invocation) {
        return Mono.defer(() -> {
            filesystemBeforeCall.run();
            return invocation.get();
        });
    }

    private FilesystemPreparation validateAndPrepareFilesystem(
            AgentConfig config, String agentNamespace) {
        HarnessConfig harness = config.getHarness();
        if (harness == null) {
            throw new AgentConfigException("liteflow.agent.harness must not be null");
        }
        try {
            harness.validate();
        }
        catch (IllegalStateException failure) {
            throw new AgentConfigException(failure.getMessage(), failure);
        }
        HarnessFilesystemBackend backend = harness.getFilesystemBackend();
        HarnessFilesystemConfigurer configurer;
        if (backend == HarnessFilesystemBackend.DOCKER) {
            configurer = new DockerSandboxConfigurer(
                    sandboxSnapshotProvider(), dockerSandboxClient());
        }
        else if (backend == HarnessFilesystemBackend.GUARDED_LOCAL) {
            configurer = new GuardedLocalFilesystemConfigurer(agentNamespace);
        }
        else {
            configurer = filesystemConfigurer();
            if (configurer == null) {
                throw new AgentConfigException(
                        "Harness CUSTOM filesystem requires a non-null filesystemConfigurer");
            }
        }
        if (config.getWorkspace() == null) {
            throw new AgentConfigException("liteflow.agent.workspace must not be null");
        }
        String root = config.getWorkspace().getRoot();
        if (root == null || root.isBlank()) {
            throw new AgentConfigException("Harness workspace.root must not be blank");
        }
        if (config.getWorkspace().getMaxFileBytes() <= 0) {
            throw new AgentConfigException("Harness workspace.maxFileBytes must be positive");
        }
        if (config.getShell() == null) {
            throw new AgentConfigException("liteflow.agent.shell must not be null");
        }
        Duration commandTimeout = config.getShell().getTimeout();
        if (commandTimeout == null || commandTimeout.isZero() || commandTimeout.isNegative()) {
            throw new AgentConfigException("Harness shell.timeout must be positive");
        }
        try {
            HarnessFilesystemContext context = new HarnessFilesystemContext(
                    Path.of(root).toAbsolutePath().normalize(),
                    config.getWorkspace().getMaxFileBytes(),
                    commandTimeout,
                    config);
            Runnable beforeCall = backend == HarnessFilesystemBackend.DOCKER
                    ? DockerSandboxConfigurer.workspaceProjectionPreflight(context)
                    : () -> { };
            return new FilesystemPreparation(
                    configurer,
                    context,
                    beforeCall,
                    backend == HarnessFilesystemBackend.DOCKER);
        }
        catch (InvalidPathException failure) {
            throw new AgentConfigException("invalid Harness workspace.root", failure);
        }
    }

    private void collectOwnedHarnessResources(List<AutoCloseable> resources) {
        List<? extends AutoCloseable> configured = ownedHarnessResources();
        if (configured == null) {
            throw new AgentConfigException("ownedHarnessResources must not return null");
        }
        for (AutoCloseable resource : configured) {
            if (resource == null) {
                throw new AgentConfigException("ownedHarnessResources must not contain null");
            }
            addIdentityDistinct(resources, resource);
        }
    }

    private static void requireNoReservedRuntimeValues(RuntimeContext context) {
        List<String> supplied = new ArrayList<>();
        addIfPresent(context, SandboxContext.class, supplied);
        addIfPresent(context, AbstractFilesystem.class, supplied);
        addIfPresent(context, WorkspaceManager.class, supplied);
        addIfPresent(context, WorkspacePathNormalizer.class, supplied);
        if (!supplied.isEmpty()) {
            throw new AgentConfigException(
                    "customizeRuntimeContext must not supply Harness-reserved values: "
                            + String.join(", ", supplied));
        }
    }

    private static <T> void addIfPresent(
            RuntimeContext context, Class<T> type, List<String> supplied) {
        if (context.get(type) != null) {
            supplied.add(type.getSimpleName());
        }
    }

    private List<SubagentDeclaration> requireSubagents() {
        List<SubagentDeclaration> configured = subagents();
        if (configured == null) {
            throw new AgentConfigException("subagents must not return null");
        }
        List<SubagentDeclaration> declarations = new ArrayList<>();
        Set<SubagentDeclaration> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (SubagentDeclaration declaration : configured) {
            if (declaration == null) {
                throw new AgentConfigException("subagents must not contain null");
            }
            if (seen.add(declaration)) {
                declarations.add(declaration);
            }
        }
        return List.copyOf(declarations);
    }

    private List<String> requireAdditionalContextFiles() {
        List<String> configured = additionalContextFiles();
        if (configured == null) {
            throw new AgentConfigException("additional context files must not return null");
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String file : configured) {
            if (file == null || file.isBlank()) {
                throw new AgentConfigException("additional context files must not contain blank values");
            }
            if (!seen.add(file)) {
                throw new AgentConfigException("additional context files must not contain duplicates: " + file);
            }
        }
        return List.copyOf(seen);
    }

    private static void addLiteFlowMiddlewares(
            HarnessAgent.Builder builder, PreparedAgentResources prepared) {
        List<MiddlewareBase> core = prepared.coreMiddlewares();
        for (int index = 0; index < core.size() - 2; index++) {
            builder.middleware(core.get(index));
        }
        for (MiddlewareBase middleware : prepared.userMiddlewares()) {
            builder.middleware(AgentMiddlewareOrder.user(middleware));
        }
        builder.middleware(core.get(core.size() - 2));
        builder.middleware(core.get(core.size() - 1));
    }

    private static void addIdentityDistinct(
            List<AutoCloseable> resources, AutoCloseable candidate) {
        for (AutoCloseable resource : resources) {
            if (resource == candidate) {
                return;
            }
        }
        resources.add(candidate);
    }

    private record FilesystemPreparation(
            HarnessFilesystemConfigurer configurer,
            HarnessFilesystemContext context,
            Runnable beforeCall,
            boolean dockerBackend) {
    }
}
