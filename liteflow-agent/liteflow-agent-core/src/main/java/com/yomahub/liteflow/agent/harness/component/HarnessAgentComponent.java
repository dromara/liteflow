package com.yomahub.liteflow.agent.harness.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.component.AbstractAgentScopeComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.GuardedLocalFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.agent.harness.filesystem.LocalExecutionFilesystem;
import com.yomahub.liteflow.agent.harness.tool.SandboxShellTool;
import io.agentscope.harness.agent.filesystem.sandbox.AbstractSandboxFilesystem;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.harness.runtime.SandboxCallGate;
import com.yomahub.liteflow.agent.harness.sandbox.DockerSandboxConfigurer;
import com.yomahub.liteflow.agent.harness.sandbox.SandboxSnapshotProvider;
import com.yomahub.liteflow.agent.harness.sandbox.SessionSandboxRegistry;
import com.yomahub.liteflow.agent.harness.storage.HarnessStorage;
import com.yomahub.liteflow.agent.harness.storage.ManagedSandboxFilesystem;
import com.yomahub.liteflow.agent.harness.storage.StaticWorkspaceStaging;
import com.yomahub.liteflow.agent.harness.storage.StoreSnapshotClient;
import com.yomahub.liteflow.property.agent.AgentSessionStoreType;
import io.agentscope.harness.agent.sandbox.snapshot.RemoteSnapshotSpec;
import com.yomahub.liteflow.agent.harness.storage.StoredWorkspaceFilesystem;
import io.agentscope.harness.agent.middleware.HarnessSkillMiddleware;
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
import com.yomahub.liteflow.property.agent.DockerSandboxLifecycle;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.core.permission.PermissionMode;
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
        extends AbstractAgentScopeComponent<HarnessAgentRuntime> {

    private volatile Runnable filesystemBeforeCall = () -> { };

    protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
        return builder;
    }

    /** @deprecated Configure liteflow.agent.harness.compaction-threshold instead. */
    @Deprecated
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
    protected final void registerShellTool(io.agentscope.core.tool.Toolkit toolkit, AgentConfig config) {
        // Harness owns the backend's execute tool. Never register the core host command tool here.
        validateShellToolConfiguration(config);
    }

    @Override
    protected final boolean requiresWorkspaceLease() {
        return true;
    }

    @Override
    protected final HarnessNamespacedAgentStateStore createNamespacedStateStore(
            AgentStateStore delegate, String agentNamespace, String applicationName) {
        return new HarnessNamespacedAgentStateStore(delegate, agentNamespace, applicationName);
    }

    @Override
    protected HarnessAgentRuntime buildRuntime(AgentRuntimeBuildContext buildContext) {
        boolean shellEnabled = enableShellTool();
        FilesystemPreparation filesystem = validateAndPrepareFilesystem(
                buildContext.agentConfig(), buildContext.agentNamespace());
        PreparedAgentResources prepared = prepareAgentResources(buildContext);
        boolean customExecuteTool = prepared.requiredTools().containsKey("execute");
        HarnessAgent agent = null;
        List<AutoCloseable> ownedProviderResources = new ArrayList<>();
        AutoCloseable workspaceTaskRollback = null;
        try {
            collectOwnedHarnessResources(ownedProviderResources);
            ManagedSandboxFilesystem managedFilesystem = null;
            if (usesSharedStorage(buildContext.agentConfig())) {
                AgentConfig config = buildContext.agentConfig();
                HarnessStorage storage = HarnessStorage.open(config.getSessionStore());
                ownedProviderResources.add(storage);
                Path executionRoot = filesystem.context().workspaceRoot();
                StaticWorkspaceStaging staging = StaticWorkspaceStaging.create(config);
                ownedProviderResources.add(staging);
                Path sharedRoot = staging.root();
                if (!filesystem.dockerBackend()) {
                    new com.yomahub.liteflow.agent.harness.filesystem.GuardedLocalFilesystem(
                            executionRoot.resolve(config.getApplicationName()));
                }
                HarnessFilesystemContext fsContext = new HarnessFilesystemContext(sharedRoot, filesystem.context().commandTimeout(), config);
                HarnessFilesystemConfigurer configurer;
                if (filesystem.dockerBackend()) {
                    managedFilesystem = new ManagedSandboxFilesystem(storage.store(), config.getApplicationName(),
                            buildContext.agentNamespace(), sharedRoot, config.getHarness().getDocker().getWorkspaceRoot());
                    configurer = new DockerSandboxConfigurer(ignored -> new RemoteSnapshotSpec(
                            new StoreSnapshotClient(storage.store(), config.getApplicationName())),
                            dockerSandboxClient(), managedFilesystem);
                } else {
                    var remote = new StoredWorkspaceFilesystem(storage.store(), rc -> rc.getSessionId() == null
                            ? List.of("liteflow", config.getApplicationName(), "workspace-v2-internal", buildContext.agentNamespace())
                            : List.of("liteflow", config.getApplicationName(), "workspace-v2", rc.getSessionId()), sharedRoot.resolve(config.getApplicationName()));
                    configurer = (builder, context) -> {
                        var files = enableShellTool()
                                ? new LocalExecutionFilesystem(remote, executionRoot,
                                        config.getHarness().getShell(), config.getApplicationName())
                                : remote;
                        builder.abstractFilesystem(files).transcriptStore(
                                new io.agentscope.harness.agent.transcript.ObjectStoreTranscriptStore(
                                        files, RuntimeContext.empty(), ".agentscope/transcripts"));
                    };
                }
                filesystem = new FilesystemPreparation(configurer, fsContext,
                        filesystem.dockerBackend() ? DockerSandboxConfigurer.workspaceProjectionPreflight(fsContext) : () -> { },
                        filesystem.dockerBackend());
            }
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

            java.util.concurrent.atomic.AtomicReference<HarnessAgent> workspaceAgent = new java.util.concurrent.atomic.AtomicReference<>();
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
                    .workspace(filesystem.dockerBackend() ? filesystem.context().workspaceRoot()
                            : filesystem.context().workspaceRoot().resolve(buildContext.agentConfig().getApplicationName()));
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
            com.yomahub.liteflow.agent.harness.compaction.AdaptiveCompactionMiddleware adaptiveCompaction = null;
            if (compaction != null) {
                builder.compaction(compaction);
            } else {
                builder.disableCompaction();
                HarnessConfig harnessConfig = buildContext.agentConfig().getHarness();
                adaptiveCompaction = new com.yomahub.liteflow.agent.harness.compaction.AdaptiveCompactionMiddleware(
                        () -> workspaceAgent.get().getWorkspaceManager(), prepared.defaultModel(),
                        prepared.fallbackModel(), prepared.managedModels(), harnessConfig.getCompactionThreshold(),
                        harnessConfig.getCompactionFallbackContextWindow(), harnessConfig.getCompactionFallbackThreshold());
                builder.middleware(adaptiveCompaction);
            }
            MemoryConfig memory = HarnessMemoryConfigResolver.resolve(
                    memoryConfig(), buildContext.agentConfig().getHarness().getMemory());
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
                    HarnessAgentBuilderPermissionBridge.defaultBypass(
                            prepared.permissionContext());
            boolean planMode = enablePlanMode();
            builder.enableTaskList()
                    .enablePlanMode(planMode)
                    .permissionContext(permissionContext);
            ToolResultEvictionConfig eviction = toolResultEvictionConfig();
            if (eviction != null) {
                builder.toolResultEviction(eviction);
            }
            addLiteFlowMiddlewares(builder, prepared);
            builder.middleware(new com.yomahub.liteflow.agent.harness.skill.SessionSkillWorkspaceMiddleware(
                    () -> workspaceAgent.get().getWorkspaceManager().getFilesystem(), shellEnabled));
            HarnessAgentBuilderCapabilitiesBridge.CapabilitiesSnapshot capabilitySnapshot =
                    HarnessAgentBuilderCapabilitiesBridge.snapshot(builder);
            HarnessAgentBuilderPermissionBridge.PermissionSnapshot permissionSnapshot =
                    HarnessAgentBuilderPermissionBridge.snapshot(builder, permissionContext);

            if (!shellEnabled || customExecuteTool) builder.disableShellTool();
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
                    true,
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
            if (!shellEnabled || customExecuteTool) customized.disableShellTool();
            agent = customized.build();
            workspaceAgent.set(agent);
            if (adaptiveCompaction != null && !agent.getDelegate().getMiddlewares().contains(adaptiveCompaction)) {
                throw new AgentConfigException("customizeHarness must retain adaptive compaction middleware");
            }
            if (agent.getDelegate().getMiddlewares().stream().anyMatch(
                    io.agentscope.core.skill.DynamicSkillMiddleware.class::isInstance)) {
                throw new AgentConfigException("DynamicSkillMiddleware conflicts with the Harness skill runtime");
            }
            if (!shellEnabled) {
                if (!customExecuteTool) agent.getToolkit().removeTool("execute");
            } else if (!customExecuteTool && agent.getToolkit().getToolNames().contains("execute")) {
                var executionFilesystem = agent.getWorkspaceManager().getFilesystem();
                if (executionFilesystem instanceof AbstractSandboxFilesystem sandbox) {
                    // Preserve SDK Skill discovery; enforce the configured policy on the selected backend.
                    agent.getToolkit().removeTool("execute");
                    if (executionFilesystem instanceof LocalExecutionFilesystem local) {
                        agent.getToolkit().registerTool(local);
                    } else {
                        agent.getToolkit().registerTool(new SandboxShellTool(sandbox,
                                buildContext.agentConfig().getHarness().getShell(),
                                buildContext.agentConfig().getHarness().getFilesystemBackend() == HarnessFilesystemBackend.DOCKER));
                    }
                }
            }
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
            SessionSandboxRegistry sandboxRegistry = null;
            if (filesystem.dockerBackend() && (managedFilesystem != null || buildContext.agentConfig().getHarness().getDocker().getLifecycle()
                    == DockerSandboxLifecycle.SESSION_IDLE)) {
                sandboxRegistry = new SessionSandboxRegistry(
                        ((DockerSandboxConfigurer) filesystem.configurer()).sandboxContext(),
                        prepared.ownership().stateStore(), buildContext.agentNamespace(),
                        invocationGuard(), buildContext.agentConfig().getHarness().getDocker(), managedFilesystem,
                        prestageSkills(agent));
                ownedProviderResources.add(sandboxRegistry);
            }
            return new HarnessAgentRuntime(
                    agent,
                    prepared.ownership(),
                    ownedProviderResources,
                    sandboxCallGate,
                    permissionContext,
                    sandboxRegistry);
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
        var permissionContext = runtime.permissionContext();
        HarnessRuntimeContextContinuation continuation =
                HarnessRuntimeContextContinuation.create();
        runtimeContext.put(HarnessRuntimeContextContinuation.class, continuation);
        Mono<Msg> invocation = Mono.defer(() -> {
        if (permissionContext != null && permissionContext.getMode() == PermissionMode.BYPASS) {
            agent.getDelegate().replacePermissionContext(
                    runtimeContext.getUserId(), runtimeContext.getSessionId(), permissionContext);
        }
        return runtime.executeSandboxCall(liteflowContext.getIdentity(), runtimeContext, () -> invokeCallTarget(
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
        });
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
            configurer = new GuardedLocalFilesystemConfigurer(agentNamespace, enableShellTool());
        }
        else {
            configurer = filesystemConfigurer();
            if (configurer == null) {
                throw new AgentConfigException(
                        "Harness CUSTOM filesystem requires a non-null filesystemConfigurer");
            }
        }
        String root;
        if (backend == HarnessFilesystemBackend.DOCKER) {
            // Host state/cache is separate from the container execution location.
            String persistent = config.getSessionStore().getJsonWorkspaceRoot();
            root = usesSharedStorage(config) ? "." : persistent == null || persistent.isBlank()
                    ? Path.of(config.getSessionStore().getJsonRoot()).resolve("workspace").toString()
                    : persistent;
        } else {
            root = harness.getLocal().getWorkspaceRoot();
            if (root == null || root.isBlank()) {
                if (enableShellTool()) {
                    throw new AgentConfigException("harness.local.workspace-root is required for local execution");
                }
                root = usesSharedStorage(config) ? "."
                        : Path.of(config.getSessionStore().getJsonRoot()).resolve("workspace").toString();
            }
        }
        if (usesSharedStorage(config)) {
            if (backend == HarnessFilesystemBackend.CUSTOM) throw new AgentConfigException(
                    "Shared storage requires the built-in DOCKER or GUARDED_LOCAL filesystem");
            if (backend == HarnessFilesystemBackend.DOCKER
                    && (harness.getDocker().getSnapshotRoot() != null && !harness.getDocker().getSnapshotRoot().isBlank()
                    || sandboxSnapshotProvider() != null)) throw new AgentConfigException(
                    "MYSQL/REDIS storage owns snapshots; remove docker.snapshot-root and SandboxSnapshotProvider");
        }
        if (config.getHarness().getShell() == null) {
            throw new AgentConfigException("liteflow.agent.harness.shell must not be null");
        }
        Duration commandTimeout = config.getHarness().getShell().getTimeout();
        if (commandTimeout == null || commandTimeout.isZero() || commandTimeout.isNegative()) {
            throw new AgentConfigException("liteflow.agent.harness.shell.timeout must be positive");
        }
        try {
            HarnessFilesystemContext context = new HarnessFilesystemContext(
                    Path.of(root).toAbsolutePath().normalize(),
                    commandTimeout,
                    config);
            if (backend == HarnessFilesystemBackend.DOCKER && !usesSharedStorage(config)) {
                new com.yomahub.liteflow.agent.harness.filesystem.GuardedLocalFilesystem(
                        context.workspaceRoot());
            }
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
            throw new AgentConfigException("invalid Harness workspace location", failure);
        }
    }

    private static boolean usesSharedStorage(AgentConfig config) {
        return config.getSessionStore().getType() == AgentSessionStoreType.MYSQL
                || config.getSessionStore().getType() == AgentSessionStoreType.REDIS;
    }

    private static java.util.function.Consumer<RuntimeContext> prestageSkills(HarnessAgent agent) {
        var skills = agent.getDelegate().getMiddlewares().stream()
                .filter(HarnessSkillMiddleware.class::isInstance).map(HarnessSkillMiddleware.class::cast).toList();
        return context -> skills.forEach(skill -> skill.prestageMarketplaceSkills(context));
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
