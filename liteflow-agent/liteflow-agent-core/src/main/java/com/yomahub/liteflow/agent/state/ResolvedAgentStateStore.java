package com.yomahub.liteflow.agent.state;

import com.yomahub.liteflow.agent.exception.AgentException;
import io.agentscope.core.state.AgentStateStore;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** An AgentStateStore together with the ownership decision made by its resolver. */
public final class ResolvedAgentStateStore implements AutoCloseable {

    private final AgentStateStore store;
    private final boolean owned;
    private final AtomicBoolean closed = new AtomicBoolean();

    public ResolvedAgentStateStore(AgentStateStore store, boolean owned) {
        this.store = Objects.requireNonNull(store, "store");
        this.owned = owned;
    }

    public AgentStateStore store() {
        return store;
    }

    public boolean owned() {
        return owned;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true) || !owned) {
            return;
        }
        try {
            store.close();
        } catch (RuntimeException | Error failure) {
            throw new AgentException("Failed to close owned AgentStateStore", failure);
        }
    }
}
