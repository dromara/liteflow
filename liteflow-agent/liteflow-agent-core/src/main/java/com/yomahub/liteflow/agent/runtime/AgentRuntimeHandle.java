package com.yomahub.liteflow.agent.runtime;

import com.yomahub.liteflow.agent.exception.AgentException;

import java.util.Objects;
import java.util.function.Supplier;

/** Component-owned, lazy and retryable runtime lifecycle handle. */
public final class AgentRuntimeHandle<R extends AutoCloseable> implements AutoCloseable {

    private R runtime;
    private boolean initialized;
    private boolean closed;

    public synchronized R getOrCreate(Supplier<? extends R> factory) {
        Objects.requireNonNull(factory, "factory");
        if (closed) {
            throw new IllegalStateException("Agent runtime handle is closed");
        }
        if (runtime != null) {
            return runtime;
        }

        R built = Objects.requireNonNull(factory.get(), "runtime factory returned null");
        if (closed) {
            IllegalStateException failure = new IllegalStateException(
                    "Agent runtime handle was closed during creation");
            try {
                closeRuntime(built);
            } catch (AgentException closeFailure) {
                failure.addSuppressed(closeFailure);
            }
            throw failure;
        }
        runtime = built;
        initialized = true;
        return built;
    }

    public synchronized boolean isInitialized() {
        return initialized;
    }

    public synchronized boolean isClosed() {
        return closed;
    }

    @Override
    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        R toClose = runtime;
        runtime = null;
        if (toClose != null) {
            closeRuntime(toClose);
        }
    }

    private static void closeRuntime(AutoCloseable runtime) {
        try {
            runtime.close();
        } catch (AgentException failure) {
            throw failure;
        } catch (Exception | LinkageError failure) {
            throw new AgentException("Failed to close Agent runtime", failure);
        }
    }
}
