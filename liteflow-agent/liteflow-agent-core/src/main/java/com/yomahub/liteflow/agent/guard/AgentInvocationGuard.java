package com.yomahub.liteflow.agent.guard;

import java.time.Duration;

/** Acquires a lease that protects an invocation resource. */
public interface AgentInvocationGuard extends AutoCloseable {
    AgentInvocationLease acquire(AgentInvocationKey key, Duration timeout);
    @Override default void close() { }
}
