package com.yomahub.liteflow.agent.harness.component;

import com.fasterxml.jackson.databind.JsonNode;
import com.yomahub.liteflow.agent.component.AbstractReActLikeAgentComponent;
import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.harness.filesystem.GuardedLocalFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemConfigurer;
import com.yomahub.liteflow.agent.harness.filesystem.HarnessFilesystemContext;
import com.yomahub.liteflow.agent.harness.runtime.HarnessAgentRuntime;
import com.yomahub.liteflow.agent.hitl.AgentCallTarget;
import com.yomahub.liteflow.agent.message.AgentOutputSpec;
import com.yomahub.liteflow.agent.middleware.AgentMiddlewareOrder;
import com.yomahub.liteflow.agent.runtime.AgentRuntimeBuildContext;
import com.yomahub.liteflow.agent.runtime.PreparedAgentResources;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.HarnessConfig;
import com.yomahub.liteflow.property.agent.HarnessFilesystemBackend;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.message.Msg;
import io.agentscope.core.middleware.MiddlewareBase;
import io.agentscope.harness.agent.HarnessAgent;
import io.agentscope.harness.agent.memory.MemoryConfig;
import io.agentscope.harness.agent.memory.compaction.CompactionConfig;
import io.agentscope.harness.agent.memory.compaction.ToolResultEvictionConfig;
import io.agentscope.harness.agent.subagent.SubagentDeclaration;
import io.agentscope.harness.agent.subagent.task.TaskRepository;
import io.agentscope.harness.agent.subagent.task.WorkspaceTaskRepository;
import reactor.core.publisher.Mono;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/** AgentScope Harness specialization using the shared LiteFlow component lifecycle. */
public abstract class HarnessAgentComponent
        extends AbstractReActLikeAgentComponent<HarnessAgentRuntime> {

    protected HarnessAgent.Builder customizeHarness(HarnessAgent.Builder builder) {
        return builder;
    }

    protected CompactionConfig compactionConfig() {
        return null;
    }

    protected MemoryConfig memoryConfig() {
        return null;
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
            builder.compaction(compactionConfig())
                    .memory(memoryConfig());
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
            builder.enableTaskList()
                    .enablePlanMode(enablePlanMode())
                    .permissionContext(prepared.permissionContext())
                    .toolResultEviction(toolResultEvictionConfig());
            addLiteFlowMiddlewares(builder, prepared);

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
            if (guardedLocal) {
                HarnessAgentBuilderFilesystemBridge.requireGuardedLocalSubagentsSafe(
                        customized, prepared.toolkit());
            }
            HarnessAgentBuilderTaskOwnershipBridge.TaskOwnershipSnapshot taskOwnership =
                    HarnessAgentBuilderTaskOwnershipBridge.snapshot(customized, tasks);
            HarnessAgentBuilderFilesystemBridge.preflightKnownBuildFailures(
                    customized, filesystemSnapshot);
            agent = customized.build();
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
            return new HarnessAgentRuntime(agent, prepared.ownership(), ownedProviderResources);
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
        return invokeCallTarget(
                new AgentCallTarget() {
                    @Override
                    public Mono<Msg> call(List<Msg> messages, RuntimeContext context) {
                        return agent.call(messages, context);
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, Class<?> type, RuntimeContext context) {
                        return agent.call(messages, type, context);
                    }

                    @Override
                    public Mono<Msg> call(
                            List<Msg> messages, JsonNode schema, RuntimeContext context) {
                        return agent.call(messages, schema, context);
                    }
                },
                runtime.stateStore(),
                input,
                output,
                runtimeContext,
                liteflowContext);
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
            throw new AgentConfigException(
                    "Harness DOCKER filesystem is not implemented until Task 5");
        }
        if (backend == HarnessFilesystemBackend.GUARDED_LOCAL) {
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
            return new FilesystemPreparation(configurer, context);
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
            HarnessFilesystemContext context) {
    }
}
