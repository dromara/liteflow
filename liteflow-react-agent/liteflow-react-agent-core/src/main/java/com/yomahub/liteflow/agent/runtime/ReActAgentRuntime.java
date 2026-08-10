package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.ReActAgent;
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

/** Owns one stateless ReActAgent and the Task-3 resources used to build it. */
public final class ReActAgentRuntime implements AutoCloseable {

    private final ReActAgent agent;
    private final GuardedNamespacedAgentStateStore stateStore;
    private final ResolvedAgentStateStore resolvedStateStore;
    private final List<McpClientWrapper> ownedMcpClients;
    private final List<AgentSkillRepository> ownedSkillRepositories;
    private final List<Model> ownedModels;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ReActAgentRuntime(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<? extends Model> ownedModels) {
        this(agent, stateStore, resolvedStateStore, List.of(), List.of(), ownedModels);
    }

    public ReActAgentRuntime(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<McpClientRegistration> mcpClients,
            List<? extends AgentSkillRepository> ownedSkillRepositories,
            List<? extends Model> ownedModels) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.resolvedStateStore = Objects.requireNonNull(
                resolvedStateStore, "resolvedStateStore");
        this.ownedMcpClients = ownedMcpClients(mcpClients);
        this.ownedSkillRepositories = identityDistinct(
                ownedSkillRepositories, "ownedSkillRepositories");
        this.ownedModels = identityDistinct(ownedModels, "ownedModels");
    }

    public ReActAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return stateStore;
    }

    /** Close order is Agent, owned MCP/repositories/models, then StateStore wrappers. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = closeResource(agent, null);
        for (int index = ownedMcpClients.size() - 1; index >= 0; index--) {
            failure = closeResource(ownedMcpClients.get(index), failure);
        }
        for (int index = ownedSkillRepositories.size() - 1; index >= 0; index--) {
            failure = closeResource(ownedSkillRepositories.get(index), failure);
        }
        for (int index = ownedModels.size() - 1; index >= 0; index--) {
            failure = closeResource(ownedModels.get(index), failure);
        }
        failure = closeResource(stateStore, failure);
        failure = closeResource(resolvedStateStore, failure);
        if (failure != null) {
            if (failure instanceof AgentException agentFailure) {
                throw agentFailure;
            }
            throw new AgentException("Failed to close ReAct Agent runtime", failure);
        }
    }

    private static Throwable closeResource(Object resource, Throwable existingFailure) {
        if (!(resource instanceof AutoCloseable closeable)) {
            return existingFailure;
        }
        try {
            closeable.close();
        } catch (Throwable closeFailure) {
            if (existingFailure == null) {
                return closeFailure;
            }
            existingFailure.addSuppressed(closeFailure);
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
