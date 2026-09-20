package com.yomahub.liteflow.test.agent.feature.harness;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DockerProcessRunnerTest {

    private final DockerProcessRunner runner = new DockerProcessRunner(25, 25, 25);

    @Test
    void finallyDestroysProcessStillAliveAfterCommandCompletion() throws Exception {
        StubProcess process = StubProcess.completedButAlive();

        DockerCommandResult result = runner.run(ignored -> process, List.of("fake", "success"));

        assertEquals(0, result.exitCode());
        assertTrue(process.destroyCalled);
        assertFalse(process.isAlive());
    }

    @Test
    void finallyDestroysProcessWhenReaderSetupFails() {
        StubProcess process = StubProcess.inputAccessFails();

        assertThrows(IllegalStateException.class,
                () -> runner.run(ignored -> process, List.of("fake", "reader-setup")));

        assertTrue(process.destroyCalled);
        assertFalse(process.isAlive());
    }

    @Test
    void timeoutEscalatesFromDestroyToDestroyForcibly() {
        StubProcess process = StubProcess.neverCompletesUntilForced();

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> runner.run(ignored -> process, List.of("fake", "timeout")));

        assertTrue(failure.getMessage().contains("timed out"));
        assertTrue(process.destroyCalled);
        assertTrue(process.destroyForciblyCalled);
        assertFalse(process.isAlive());
    }

    @Test
    void cleanupFailureIsSuppressedBehindPrimaryOutputFailure() {
        StubProcess process = StubProcess.completedButUnkillable(
                new FailingInputStream("primary output failure"));

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> runner.run(ignored -> process, List.of("fake", "broken-output")));

        assertTrue(failure.getMessage().contains("process output"));
        assertTrue(failure.getCause().getMessage().contains("primary output failure"));
        assertEquals(1, failure.getSuppressed().length);
        assertTrue(failure.getSuppressed()[0].getMessage().contains("could not terminate"));
        assertTrue(process.destroyCalled);
        assertTrue(process.destroyForciblyCalled);
    }

    @Test
    void boundedReaderJoinInterruptsReaderThatDoesNotFinish() throws Exception {
        InterruptibleBlockingInputStream blocked = new InterruptibleBlockingInputStream();
        StubProcess process = StubProcess.completed(blocked);

        IllegalStateException failure = assertThrows(IllegalStateException.class,
                () -> runner.run(ignored -> process, List.of("fake", "blocked-reader")));

        assertTrue(failure.getMessage().contains("reader did not terminate"));
        assertTrue(blocked.interrupted.await(1, TimeUnit.SECONDS));
    }

    @Test
    void interruptedCommandWaitRestoresCallerInterruptFlag() {
        StubProcess process = StubProcess.interruptedWait();
        try {
            assertThrows(InterruptedException.class,
                    () -> runner.run(ignored -> process, List.of("fake", "interrupted")));
            assertTrue(Thread.currentThread().isInterrupted());
        }
        finally {
            Thread.interrupted();
        }
    }

    private static final class StubProcess extends Process {

        private final boolean commandCompletes;
        private final boolean destroyTerminates;
        private final boolean forceTerminates;
        private final boolean waitInterrupted;
        private final InputStream stdout;
        private boolean alive;
        private boolean destroyCalled;
        private boolean destroyForciblyCalled;
        private boolean inputAccessFails;

        private StubProcess(
                boolean commandCompletes,
                boolean alive,
                boolean destroyTerminates,
                boolean forceTerminates,
                boolean waitInterrupted,
                InputStream stdout) {
            this.commandCompletes = commandCompletes;
            this.alive = alive;
            this.destroyTerminates = destroyTerminates;
            this.forceTerminates = forceTerminates;
            this.waitInterrupted = waitInterrupted;
            this.stdout = stdout;
        }

        static StubProcess completed(InputStream stdout) {
            return new StubProcess(true, false, true, true, false, stdout);
        }

        static StubProcess completedButAlive() {
            return new StubProcess(
                    true, true, true, true, false, new ByteArrayInputStream(new byte[0]));
        }

        static StubProcess inputAccessFails() {
            StubProcess process = completedButAlive();
            process.inputAccessFails = true;
            return process;
        }

        static StubProcess neverCompletesUntilForced() {
            return new StubProcess(
                    false, true, false, true, false, new ByteArrayInputStream(new byte[0]));
        }

        static StubProcess completedButUnkillable(InputStream stdout) {
            return new StubProcess(true, true, false, false, false, stdout);
        }

        static StubProcess interruptedWait() {
            return new StubProcess(
                    true, false, true, true, true, new ByteArrayInputStream(new byte[0]));
        }

        @Override
        public OutputStream getOutputStream() {
            return OutputStream.nullOutputStream();
        }

        @Override
        public InputStream getInputStream() {
            if (inputAccessFails) {
                throw new IllegalStateException("stdout unavailable");
            }
            return stdout;
        }

        @Override
        public InputStream getErrorStream() {
            return new ByteArrayInputStream(new byte[0]);
        }

        @Override
        public int waitFor() {
            alive = false;
            return 0;
        }

        @Override
        public boolean waitFor(long timeout, TimeUnit unit) throws InterruptedException {
            if (waitInterrupted) {
                throw new InterruptedException("command wait interrupted");
            }
            return commandCompletes || !alive;
        }

        @Override
        public int exitValue() {
            return 0;
        }

        @Override
        public void destroy() {
            destroyCalled = true;
            if (destroyTerminates) {
                alive = false;
            }
        }

        @Override
        public Process destroyForcibly() {
            destroyForciblyCalled = true;
            if (forceTerminates) {
                alive = false;
            }
            return this;
        }

        @Override
        public boolean isAlive() {
            return alive;
        }
    }

    private static final class FailingInputStream extends InputStream {

        private final String message;

        private FailingInputStream(String message) {
            this.message = message;
        }

        @Override
        public int read() throws IOException {
            throw new IOException(message);
        }
    }

    private static final class InterruptibleBlockingInputStream extends InputStream {

        private final CountDownLatch release = new CountDownLatch(1);
        private final CountDownLatch interrupted = new CountDownLatch(1);

        @Override
        public int read() throws IOException {
            try {
                release.await();
                return -1;
            }
            catch (InterruptedException failure) {
                interrupted.countDown();
                Thread.currentThread().interrupt();
                throw new IOException("reader interrupted", failure);
            }
        }

        @Override
        public void close() {
            release.countDown();
        }
    }
}
