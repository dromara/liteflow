package com.yomahub.liteflow.agent.harness.sandbox;

import io.agentscope.harness.agent.sandbox.Sandbox;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CheckpointRetryTest {
    private static final String CHANGING = "docker tar command failed (exit=1): tar: .: file changed as we read it";
    private static Sandbox sandbox(AtomicInteger attempts, int failures, IOException failure) {
        return (Sandbox) Proxy.newProxyInstance(Sandbox.class.getClassLoader(), new Class<?>[]{Sandbox.class},
                (proxy, method, args) -> {
                    if (!method.getName().equals("stop")) throw new AssertionError(method.getName());
                    if (attempts.incrementAndGet() <= failures) throw failure;
                    return null;
                });
    }

    @Test void retriesRejectedChangingArchiveUntilACompleteSnapshotSucceeds() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        SessionSandboxRegistry.stopForCheckpoint(sandbox(attempts, 1, new IOException(CHANGING)));
        assertEquals(2, attempts.get());
    }

    @Test void preservesOtherFailuresWithoutRetrying() {
        AtomicInteger attempts = new AtomicInteger();
        IOException failure = new IOException("storage unavailable");
        assertSame(failure, assertThrows(IOException.class,
                () -> SessionSandboxRegistry.stopForCheckpoint(sandbox(attempts, 3, failure))));
        assertEquals(1, attempts.get());
    }

    @Test void repeatedArchiveConflictsRemainFailures() {
        AtomicInteger attempts = new AtomicInteger();
        IOException failure = new IOException(CHANGING);
        assertSame(failure, assertThrows(IOException.class,
                () -> SessionSandboxRegistry.stopForCheckpoint(sandbox(attempts, 10, failure))));
        assertEquals(3, attempts.get());
    }
}
