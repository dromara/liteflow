package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.Model;

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
    private final List<Model> ownedModels;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ReActAgentRuntime(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore,
            List<? extends Model> ownedModels) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.resolvedStateStore = Objects.requireNonNull(
                resolvedStateStore, "resolvedStateStore");
        this.ownedModels = identityDistinct(ownedModels);
    }

    public ReActAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return stateStore;
    }

    /** Close order is Agent, models in reverse build order, state decorator, ownership wrapper. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = closeResource(agent, null);
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

    private static List<Model> identityDistinct(List<? extends Model> models) {
        Objects.requireNonNull(models, "ownedModels");
        Set<Model> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        List<Model> distinct = new ArrayList<>();
        for (Model model : models) {
            Objects.requireNonNull(model, "ownedModels must not contain null");
            if (seen.add(model)) {
                distinct.add(model);
            }
        }
        return List.copyOf(distinct);
    }
}
