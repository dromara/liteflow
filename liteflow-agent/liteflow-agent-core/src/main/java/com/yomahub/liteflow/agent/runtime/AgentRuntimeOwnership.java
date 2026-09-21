package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.model.Model;
import io.agentscope.core.skill.repository.AgentSkillRepository;
import io.agentscope.core.tool.mcp.McpClientWrapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/** Provider-neutral ownership and rollback contract for an Agent runtime. */
public final class AgentRuntimeOwnership {

    private final GuardedNamespacedAgentStateStore stateStore;
    private final ResolvedAgentStateStore resolvedStateStore;
    private final List<McpClientWrapper> ownedMcpClients;
    private final List<AgentSkillRepository> ownedSkillRepositories;
    private final List<Model> ownedModels;
    private final AtomicBoolean closed = new AtomicBoolean();

    public AgentRuntimeOwnership(
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<McpClientRegistration> mcpClients,
            List<? extends AgentSkillRepository> ownedSkillRepositories,
            List<? extends Model> ownedModels) {
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.resolvedStateStore = Objects.requireNonNull(
                resolvedStateStore, "resolvedStateStore");
        this.ownedMcpClients = ownedMcpClients(mcpClients);
        this.ownedSkillRepositories = identityDistinct(
                ownedSkillRepositories, "ownedSkillRepositories");
        this.ownedModels = identityDistinct(ownedModels, "ownedModels");
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return stateStore;
    }

    /** Rolls back an owned resolved store when namespace wrapping itself could not be created. */
    public static void rollbackResolvedStateStore(
            Throwable buildFailure, ResolvedAgentStateStore resolvedStateStore) {
        rollbackPreparation(
                buildFailure,
                null,
                resolvedStateStore,
                List.of(),
                List.of(),
                List.of());
    }

    /**
     * Rolls back every resource known during preparation, before an ownership capsule exists.
     */
    public static void rollbackPreparation(
            Throwable buildFailure,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<McpClientRegistration> mcpClients,
            List<? extends AgentSkillRepository> ownedSkillRepositories,
            List<? extends Model> ownedModels) {
        Objects.requireNonNull(buildFailure, "buildFailure");
        Set<Object> closedResources = Collections.newSetFromMap(new IdentityHashMap<>());
        closeSharedResources(
                buildFailure,
                closedResources,
                ownedMcpClients(mcpClients),
                identityDistinct(ownedSkillRepositories, "ownedSkillRepositories"),
                identityDistinct(ownedModels, "ownedModels"),
                stateStore,
                Objects.requireNonNull(resolvedStateStore, "resolvedStateStore"));
    }

    /**
     * Closes the provider runtime first, then provider resources in reverse order, followed by
     * shared MCP clients, skill repositories, models, and session-store wrappers.
     */
    public void close(
            String runtimeDescription,
            Object providerRuntime,
            List<? extends AutoCloseable> providerResources) {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = closeAll(null, providerRuntime, providerResources);
        if (failure == null) {
            return;
        }
        if (failure instanceof AgentException agentFailure) {
            throw agentFailure;
        }
        throw new AgentException("Failed to close " + runtimeDescription, failure);
    }

    /** Adds every cleanup failure to the original build failure without replacing it. */
    public void rollback(
            Throwable buildFailure,
            Object providerRuntime,
            List<? extends AutoCloseable> providerResources) {
        Objects.requireNonNull(buildFailure, "buildFailure");
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        closeAll(buildFailure, providerRuntime, providerResources);
    }

    private Throwable closeAll(
            Throwable failure,
            Object providerRuntime,
            List<? extends AutoCloseable> providerResources) {
        Set<Object> closedResources = Collections.newSetFromMap(new IdentityHashMap<>());
        failure = closeDistinct(providerRuntime, closedResources, failure);
        List<? extends AutoCloseable> resources = Objects.requireNonNull(
                providerResources, "providerResources");
        for (int index = resources.size() - 1; index >= 0; index--) {
            AutoCloseable resource = Objects.requireNonNull(
                    resources.get(index), "providerResources must not contain null");
            failure = closeDistinct(resource, closedResources, failure);
        }
        return closeSharedResources(
                failure,
                closedResources,
                ownedMcpClients,
                ownedSkillRepositories,
                ownedModels,
                stateStore,
                resolvedStateStore);
    }

    private static Throwable closeSharedResources(
            Throwable failure,
            Set<Object> closedResources,
            List<McpClientWrapper> ownedMcpClients,
            List<AgentSkillRepository> ownedSkillRepositories,
            List<Model> ownedModels,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore) {
        for (int index = ownedMcpClients.size() - 1; index >= 0; index--) {
            failure = closeDistinct(
                    ownedMcpClients.get(index), closedResources, failure);
        }
        for (int index = ownedSkillRepositories.size() - 1; index >= 0; index--) {
            failure = closeDistinct(
                    ownedSkillRepositories.get(index), closedResources, failure);
        }
        for (int index = ownedModels.size() - 1; index >= 0; index--) {
            failure = closeDistinct(ownedModels.get(index), closedResources, failure);
        }
        failure = closeDistinct(stateStore, closedResources, failure);
        return closeDistinct(resolvedStateStore, closedResources, failure);
    }

    private static Throwable closeDistinct(
            Object resource, Set<Object> closedResources, Throwable existingFailure) {
        if (resource == null || !closedResources.add(resource)) {
            return existingFailure;
        }
        return closeResource(resource, existingFailure);
    }

    private static Throwable closeResource(Object resource, Throwable existingFailure) {
        if (!(resource instanceof AutoCloseable closeable)) {
            return existingFailure;
        }
        try {
            closeable.close();
        }
        catch (Throwable closeFailure) {
            if (existingFailure == null) {
                return closeFailure;
            }
            if (existingFailure != closeFailure) {
                existingFailure.addSuppressed(closeFailure);
            }
        }
        return existingFailure;
    }

    private static List<McpClientWrapper> ownedMcpClients(
            List<McpClientRegistration> registrations) {
        Objects.requireNonNull(registrations, "mcpClients");
        Set<McpClientWrapper> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<McpClientWrapper> distinct = new ArrayList<>();
        for (McpClientRegistration registration : registrations) {
            Objects.requireNonNull(registration, "mcpClients must not contain null");
            if (registration.owned() && seen.add(registration.client())) {
                distinct.add(registration.client());
            }
        }
        return List.copyOf(distinct);
    }

    private static <T> List<T> identityDistinct(List<? extends T> resources, String name) {
        Objects.requireNonNull(resources, name);
        Set<T> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<T> distinct = new ArrayList<>();
        for (T resource : resources) {
            Objects.requireNonNull(resource, name + " must not contain null");
            if (seen.add(resource)) {
                distinct.add(resource);
            }
        }
        return List.copyOf(distinct);
    }
}
