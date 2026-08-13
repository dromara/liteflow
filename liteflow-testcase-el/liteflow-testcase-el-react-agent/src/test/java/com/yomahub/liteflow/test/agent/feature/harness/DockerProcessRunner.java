package com.yomahub.liteflow.test.agent.feature.harness;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded process runner used only by the opt-in Docker integration test. */
final class DockerProcessRunner {

    private final long commandTimeoutMillis;
    private final long terminationTimeoutMillis;
    private final long readerJoinMillis;

    DockerProcessRunner(
            long commandTimeoutMillis,
            long terminationTimeoutMillis,
            long readerJoinMillis) {
        this.commandTimeoutMillis = commandTimeoutMillis;
        this.terminationTimeoutMillis = terminationTimeoutMillis;
        this.readerJoinMillis = readerJoinMillis;
    }

    static DockerProcessRunner standard() {
        return new DockerProcessRunner(15_000, 5_000, 5_000);
    }

    DockerCommandResult run(ProcessLauncher launcher, List<String> command) throws Exception {
        Process process = launcher.start(command);
        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        AtomicReference<Throwable> outputFailure = new AtomicReference<>();
        Thread stdoutReader = null;
        Thread stderrReader = null;
        Throwable primary = null;
        try {
            stdoutReader = drain(
                    process.getInputStream(), stdout, "docker-it-stdout", outputFailure);
            stderrReader = drain(
                    process.getErrorStream(), stderr, "docker-it-stderr", outputFailure);
            if (!process.waitFor(commandTimeoutMillis, TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException(
                        "Docker IT helper timed out: " + String.join(" ", command));
            }
            awaitReader(stdoutReader);
            awaitReader(stderrReader);
            if (outputFailure.get() != null) {
                throw new IllegalStateException(
                        "failed to read Docker IT process output", outputFailure.get());
            }
            return new DockerCommandResult(
                    process.exitValue(),
                    stdout.toString(StandardCharsets.UTF_8),
                    stderr.toString(StandardCharsets.UTF_8));
        }
        catch (Exception | Error failure) {
            primary = failure;
            if (failure instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw failure;
        }
        finally {
            Throwable cleanup = cleanup(process, stdoutReader, stderrReader);
            if (cleanup != null) {
                if (primary != null) {
                    primary.addSuppressed(cleanup);
                }
                else if (cleanup instanceof Exception exception) {
                    throw exception;
                }
                else if (cleanup instanceof Error error) {
                    throw error;
                }
                else {
                    throw new IllegalStateException("Docker IT helper cleanup failed", cleanup);
                }
            }
        }
    }

    private Throwable cleanup(Process process, Thread... readers) {
        Throwable failure = cleanupProcess(process);
        for (Thread reader : readers) {
            if (reader != null) {
                failure = merge(failure, cleanupReader(reader));
            }
        }
        return failure;
    }

    private Throwable cleanupProcess(Process process) {
        Throwable failure = null;
        boolean interrupted = false;
        if (process.isAlive()) {
            try {
                process.destroy();
            }
            catch (RuntimeException | Error destroyFailure) {
                failure = merge(failure, destroyFailure);
            }
        }
        if (process.isAlive()) {
            try {
                process.waitFor(terminationTimeoutMillis, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException waitFailure) {
                interrupted = true;
                failure = merge(failure, waitFailure);
            }
        }
        if (process.isAlive()) {
            try {
                process.destroyForcibly();
            }
            catch (RuntimeException | Error forceFailure) {
                failure = merge(failure, forceFailure);
            }
        }
        if (process.isAlive()) {
            try {
                process.waitFor(terminationTimeoutMillis, TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException waitFailure) {
                interrupted = true;
                failure = merge(failure, waitFailure);
            }
        }
        if (process.isAlive()) {
            failure = merge(failure, new IllegalStateException(
                    "could not terminate Docker IT helper process"));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return failure;
    }

    private void awaitReader(Thread reader) throws InterruptedException {
        reader.join(readerJoinMillis);
        if (!reader.isAlive()) {
            return;
        }
        IllegalStateException failure = new IllegalStateException(
                "Docker IT output reader did not terminate: " + reader.getName());
        reader.interrupt();
        reader.join(readerJoinMillis);
        if (reader.isAlive()) {
            failure.addSuppressed(new IllegalStateException(
                    "Docker IT output reader is still alive after interrupt: "
                            + reader.getName()));
        }
        throw failure;
    }

    private Throwable cleanupReader(Thread reader) {
        boolean interrupted = false;
        Throwable failure = null;
        try {
            reader.join(readerJoinMillis);
        }
        catch (InterruptedException joinFailure) {
            interrupted = true;
            failure = joinFailure;
        }
        if (reader.isAlive()) {
            reader.interrupt();
            try {
                reader.join(readerJoinMillis);
            }
            catch (InterruptedException joinFailure) {
                interrupted = true;
                failure = merge(failure, joinFailure);
            }
        }
        if (reader.isAlive()) {
            failure = merge(failure, new IllegalStateException(
                    "Docker IT output reader is still alive after interrupt: "
                            + reader.getName()));
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
        return failure;
    }

    private static Thread drain(
            InputStream input,
            ByteArrayOutputStream output,
            String name,
            AtomicReference<Throwable> failure) {
        Thread reader = new Thread(() -> {
            try (input; output) {
                input.transferTo(output);
            }
            catch (Exception readFailure) {
                failure.compareAndSet(null, readFailure);
            }
        }, name);
        reader.setDaemon(true);
        reader.start();
        return reader;
    }

    private static Throwable merge(Throwable primary, Throwable additional) {
        if (additional == null) {
            return primary;
        }
        if (primary == null) {
            return additional;
        }
        primary.addSuppressed(additional);
        return primary;
    }

    @FunctionalInterface
    interface ProcessLauncher {
        Process start(List<String> command) throws Exception;
    }
}

record DockerCommandResult(int exitCode, String stdout, String stderr) {
}
