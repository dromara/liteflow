package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.exception.AgentException;
import com.yomahub.liteflow.agent.state.GuardedNamespacedAgentStateStore;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import io.agentscope.core.ReActAgent;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Owns one stateless ReActAgent and the Task-3 resources used to build it. */
public final class ReActAgentRuntime implements AutoCloseable {

    private final ReActAgent agent;
    private final GuardedNamespacedAgentStateStore stateStore;
    private final ResolvedAgentStateStore resolvedStateStore;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ReActAgentRuntime(
            ReActAgent agent,
            GuardedNamespacedAgentStateStore stateStore,
            ResolvedAgentStateStore resolvedStateStore) {
        this.agent = Objects.requireNonNull(agent, "agent");
        this.stateStore = Objects.requireNonNull(stateStore, "stateStore");
        this.resolvedStateStore = Objects.requireNonNull(
                resolvedStateStore, "resolvedStateStore");
    }

    public ReActAgent agent() {
        return agent;
    }

    public GuardedNamespacedAgentStateStore stateStore() {
        return stateStore;
    }

    /** Close order is Agent, non-owning decorator, then the ownership wrapper. */
    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        Throwable failure = closeResource(agent, null);
        failure = closeResource(stateStore, failure);
        failure = closeResource(resolvedStateStore, failure);
        if (failure != null) {
            if (failure instanceof AgentException agentFailure) {
                throw agentFailure;
            }
            throw new AgentException("Failed to close ReAct Agent runtime", failure);
        }
    }

    private static Throwable closeResource(AutoCloseable resource, Throwable existingFailure) {
        try {
            resource.close();
        } catch (Throwable closeFailure) {
            if (existingFailure == null) {
                return closeFailure;
            }
            existingFailure.addSuppressed(closeFailure);
        }
        return existingFailure;
    }
}
