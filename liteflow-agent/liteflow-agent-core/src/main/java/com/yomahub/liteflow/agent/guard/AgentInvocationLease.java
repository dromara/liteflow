package com.yomahub.liteflow.agent.guard;

import java.time.Duration;
import java.util.OptionalLong;

/** A held invocation lease. */
public interface AgentInvocationLease extends AutoCloseable {
    AgentInvocationKey key();

    default OptionalLong fencingToken() {
        return OptionalLong.empty();
    }

    default void renew(Duration leaseDuration) {
        // Local leases do not expire; distributed implementations may override this.
    }

    @Override
    void close();
}
