package com.yomahub.liteflow.agent.guard;

import com.yomahub.liteflow.agent.context.AgentInvocationIdentity;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

/** Acquires workspace then state leases, and always releases them in reverse order. */
public final class AgentInvocationCoordinator {

    private final AgentInvocationGuard guard;

    public AgentInvocationCoordinator(AgentInvocationGuard guard) {
        this.guard = Objects.requireNonNull(guard, "guard");
    }

    public AgentInvocationLease acquire(AgentInvocationIdentity identity, boolean requiresWorkspaceLease, Duration timeout) {
        return requiresWorkspaceLease
                ? acquire(AgentInvocationKey.workspace(identity), AgentInvocationKey.state(identity), timeout)
                : guard.acquire(AgentInvocationKey.state(identity), timeout);
    }

    public AgentInvocationLease acquire(AgentInvocationKey workspaceKey, AgentInvocationKey stateKey, Duration timeout) {
        requireScope(workspaceKey, AgentInvocationScope.WORKSPACE);
        requireScope(stateKey, AgentInvocationScope.STATE);
        AgentInvocationLease workspaceLease = guard.acquire(workspaceKey, timeout);
        try {
            AgentInvocationLease stateLease = guard.acquire(stateKey, timeout);
            return new CombinedLease(workspaceLease, stateLease);
        } catch (RuntimeException exception) {
            try {
                workspaceLease.close();
            } catch (RuntimeException closeFailure) {
                exception.addSuppressed(closeFailure);
            }
            throw exception;
        }
    }

    private static void requireScope(AgentInvocationKey key, AgentInvocationScope expectedScope) {
        Objects.requireNonNull(key, "key");
        if (key.scope() != expectedScope) {
            throw new IllegalArgumentException("expected " + expectedScope + " key");
        }
    }

    private static final class CombinedLease implements AgentInvocationLease {
        private final AgentInvocationLease workspaceLease;
        private final AgentInvocationLease stateLease;
        private final AtomicBoolean closed = new AtomicBoolean();

        private CombinedLease(AgentInvocationLease workspaceLease, AgentInvocationLease stateLease) {
            this.workspaceLease = workspaceLease;
            this.stateLease = stateLease;
        }

        @Override
        public AgentInvocationKey key() {
            return stateLease.key();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                RuntimeException failure = null;
                try {
                    stateLease.close();
                } catch (RuntimeException exception) {
                    failure = exception;
                }
                try {
                    workspaceLease.close();
                } catch (RuntimeException exception) {
                    if (failure != null) {
                        failure.addSuppressed(exception);
                    } else {
                        failure = exception;
                    }
                }
                if (failure != null) {
                    throw failure;
                }
            }
        }
    }
}
