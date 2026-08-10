package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.agent.context.LiteFlowAgentContext;
import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** Trusted-local command tool with bounded output and process-tree cleanup. */
public class ManagedShellCommandTool {

    static final long MAX_COLLECTABLE_OUTPUT_BYTES = 16L * 1024 * 1024;
    private static final long WAIT_SLICE_NANOS = TimeUnit.MILLISECONDS.toNanos(25);
    private static final long PROCESS_CLEANUP_NANOS = TimeUnit.SECONDS.toNanos(1);

    private final GuardedWorkspacePathResolver resolver;
    private final ShellConfig shell;
    private final long timeoutNanos;
    private final int maxOutputBytes;

    public ManagedShellCommandTool(GuardedWorkspacePathResolver resolver, AgentConfig cfg) {
        this.resolver = java.util.Objects.requireNonNull(resolver, "resolver");
        if (cfg == null || cfg.getShell() == null) {
            throw new AgentConfigException("liteflow.agent.shell must not be null");
        }
        this.shell = cfg.getShell();
        this.timeoutNanos = validateTimeout(shell.getTimeout());
        this.maxOutputBytes = validateMaxOutputBytes(shell.getMaxOutputBytes());
        validateModeList(shell);
    }

    public ManagedShellCommandTool(Path root, AgentConfig cfg) {
        this(new GuardedWorkspacePathResolver(
                root,
                cfg.getWorkspace().getMaxFileBytes(),
                cfg.getWorkspace().isAutoCreate()), cfg);
    }

    @Tool(name = "execute_shell_command",
          description = "Execute a controlled command in the trusted local workspace.",
          concurrencySafe = false)
    public String executeCommand(
            RuntimeContext runtimeContext,
            @ToolParam(name = "command", description = "Single command string (pipes && || are rejected)")
            String command) {
        if (shell.getMode() == ShellMode.DISABLED) {
            return "{\"error\":\"shell execution denied by policy\"}";
        }
        if (command == null || command.isBlank()) {
            return "{\"error\":\"empty command\"}";
        }
        if (containsUnsupportedShellSyntax(command)) {
            return "{\"error\":\"unsupported shell syntax: pipes, redirection, and command chaining are not supported\"}";
        }
        String[] tokens = command.trim().split("\\s+");
        String first = tokens[0];
        if (shell.getMode() == ShellMode.WHITELIST && !shell.getWhitelist().contains(first)) {
            return "{\"error\":\"command '" + first + "' not allowed by whitelist\"}";
        }
        if (shell.getMode() == ShellMode.BLACKLIST && shell.getBlacklist().contains(first)) {
            return "{\"error\":\"command '" + first + "' not allowed by blacklist\"}";
        }

        LiteFlowAgentContext context = requireContext(runtimeContext);
        Path workspace = resolver.sessionRoot(context.getRuntimeSessionId());
        Process process = null;
        ManagedProcessTree processTree = null;
        ExecutorService outputReader = null;
        Future<OutputCapture> outputFuture = null;
        boolean interrupted = false;
        long processStartedAt = System.nanoTime();
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(tokens));
            processBuilder.directory(workspace.toFile());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            processTree = new ManagedProcessTree(process.toHandle());
            closeQuietly(process.getOutputStream());
            Process runningProcess = process;
            outputReader = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "liteflow-agent-shell-output-reader");
                thread.setDaemon(true);
                return thread;
            });
            outputFuture = outputReader.submit(
                    () -> drainOutput(runningProcess.getInputStream(), maxOutputBytes));

            WaitResult waitResult = waitForProcess(
                    process, context, processStartedAt, processTree);
            if (waitResult != WaitResult.EXITED) {
                return errorFor(waitResult);
            }
            OutputWait output = waitForOutput(outputFuture, context, processStartedAt);
            return output.capture() == null
                    ? errorFor(output.result())
                    : output.capture().render(maxOutputBytes);
        } catch (InterruptedException failure) {
            interrupted = true;
            return "{\"error\":\"shell execution interrupted\"}";
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            return "{\"error\":\"" + safeMessage(cause == null ? failure : cause) + "\"}";
        } catch (IOException failure) {
            return "{\"error\":\"" + safeMessage(failure) + "\"}";
        } finally {
            long cleanupDeadline = System.nanoTime() + PROCESS_CLEANUP_NANOS;
            if (processTree != null) {
                interrupted |= terminateProcessTree(processTree, cleanupDeadline);
            }
            if (process != null) {
                closeQuietly(process.getOutputStream());
                closeQuietly(process.getInputStream());
                closeQuietly(process.getErrorStream());
            }
            if (outputFuture != null && !outputFuture.isDone()) {
                outputFuture.cancel(true);
            }
            if (outputReader != null) {
                outputReader.shutdownNow();
                try {
                    long remaining = cleanupDeadline - System.nanoTime();
                    if (remaining > 0) {
                        outputReader.awaitTermination(remaining, TimeUnit.NANOSECONDS);
                    }
                } catch (InterruptedException failure) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private WaitResult waitForProcess(
            Process process,
            LiteFlowAgentContext context,
            long processStartedAt,
            ManagedProcessTree processTree)
            throws InterruptedException {
        while (true) {
            processTree.captureDescendantsWhileParentAlive();
            WaitWindow window = waitWindow(context, processStartedAt);
            if (window.result() != WaitResult.ACTIVE) {
                return window.result();
            }
            if (process.waitFor(window.waitNanos(), TimeUnit.NANOSECONDS)) {
                return WaitResult.EXITED;
            }
        }
    }

    private OutputWait waitForOutput(
            Future<OutputCapture> outputFuture,
            LiteFlowAgentContext context,
            long processStartedAt)
            throws InterruptedException, ExecutionException {
        while (true) {
            if (outputFuture.isDone()) {
                return new OutputWait(WaitResult.EXITED, outputFuture.get());
            }
            WaitWindow window = waitWindow(context, processStartedAt);
            if (window.result() != WaitResult.ACTIVE) {
                return new OutputWait(window.result(), null);
            }
            try {
                return new OutputWait(
                        WaitResult.EXITED,
                        outputFuture.get(window.waitNanos(), TimeUnit.NANOSECONDS));
            } catch (TimeoutException ignored) {
                // Re-check cancellation and both deadlines on the next bounded slice.
            }
        }
    }

    private WaitWindow waitWindow(LiteFlowAgentContext context, long processStartedAt) {
        if (context.isCancelled()) {
            return new WaitWindow(WaitResult.CANCELLED, 0);
        }
        Instant now = Instant.now();
        if (!now.isBefore(context.getDeadline())) {
            return new WaitWindow(WaitResult.INVOCATION_DEADLINE, 0);
        }
        long toolRemaining = timeoutNanos - (System.nanoTime() - processStartedAt);
        if (toolRemaining <= 0) {
            return new WaitWindow(WaitResult.TOOL_TIMEOUT, 0);
        }
        long invocationRemaining = nanosUntil(now, context.getDeadline());
        if (invocationRemaining <= 0) {
            return new WaitWindow(WaitResult.INVOCATION_DEADLINE, 0);
        }
        return new WaitWindow(
                WaitResult.ACTIVE,
                Math.min(WAIT_SLICE_NANOS, Math.min(toolRemaining, invocationRemaining)));
    }

    private String errorFor(WaitResult result) {
        return switch (result) {
            case CANCELLED -> "{\"error\":\"shell execution cancelled\"}";
            case INVOCATION_DEADLINE -> "{\"error\":\"invocation deadline exceeded\"}";
            case TOOL_TIMEOUT -> "{\"error\":\"timeout after " + shell.getTimeout() + "\"}";
            default -> throw new IllegalStateException("unexpected wait result: " + result);
        };
    }

    private static long nanosUntil(Instant now, Instant deadline) {
        try {
            long nanos = Duration.between(now, deadline).toNanos();
            return Math.max(0, nanos);
        } catch (ArithmeticException overflow) {
            return Long.MAX_VALUE;
        }
    }

    private static long validateTimeout(Duration timeout) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new AgentConfigException("liteflow.agent.shell.timeout must be positive");
        }
        try {
            long nanos = timeout.toNanos();
            if (nanos <= 0) {
                throw new AgentConfigException(
                        "liteflow.agent.shell.timeout must be representable in nanoseconds");
            }
            return nanos;
        } catch (ArithmeticException overflow) {
            throw new AgentConfigException(
                    "liteflow.agent.shell.timeout must be representable in nanoseconds",
                    overflow);
        }
    }

    private static int validateMaxOutputBytes(long maxOutputBytes) {
        if (maxOutputBytes <= 0 || maxOutputBytes > MAX_COLLECTABLE_OUTPUT_BYTES) {
            throw new AgentConfigException(
                    "liteflow.agent.shell.max-output-bytes must be between 1 and "
                            + MAX_COLLECTABLE_OUTPUT_BYTES);
        }
        return (int) maxOutputBytes;
    }

    private static void validateModeList(ShellConfig shell) {
        if (shell.getMode() == null) {
            throw new AgentConfigException("liteflow.agent.shell.mode must not be null");
        }
        List<String> commands = switch (shell.getMode()) {
            case WHITELIST -> shell.getWhitelist();
            case BLACKLIST -> shell.getBlacklist();
            case DISABLED -> List.of();
        };
        if (commands == null || commands.stream().anyMatch(
                command -> command == null || command.isBlank())) {
            throw new AgentConfigException(
                    "liteflow.agent.shell command list must not be null or contain blank entries");
        }
    }

    private static OutputCapture drainOutput(InputStream input, int maximum) throws IOException {
        byte[] buffer = new byte[8192];
        ByteArrayOutputStream output = new ByteArrayOutputStream(Math.min(maximum, buffer.length));
        int captured = 0;
        boolean truncated = false;
        int read;
        while ((read = input.read(buffer)) >= 0) {
            int retained = Math.min(read, maximum - captured);
            if (retained > 0) {
                output.write(buffer, 0, retained);
                captured += retained;
            }
            if (retained < read) {
                truncated = true;
            }
        }
        return new OutputCapture(output.toString(StandardCharsets.UTF_8), truncated);
    }

    private static boolean terminateProcessTree(
            ManagedProcessTree processTree, long cleanupDeadline) {
        List<ProcessHandle> handles = processTree.capturedChildFirst();
        for (ProcessHandle handle : handles) {
            destroyQuietly(handle, false);
        }
        long gracefulDeadline = System.nanoTime()
                + Math.max(0, cleanupDeadline - System.nanoTime()) / 2;
        boolean interrupted = awaitExit(handles, gracefulDeadline);
        for (ProcessHandle handle : handles) {
            destroyQuietly(handle, true);
        }
        return awaitExit(handles, cleanupDeadline) || interrupted;
    }

    private static void destroyQuietly(ProcessHandle handle, boolean forcibly) {
        try {
            if (handle.isAlive()) {
                if (forcibly) {
                    handle.destroyForcibly();
                } else {
                    handle.destroy();
                }
            }
        } catch (RuntimeException ignored) {
            // One inaccessible process must not prevent cleanup of other captured identities.
        }
    }

    private static boolean awaitExit(List<ProcessHandle> handles, long deadline) {
        boolean interrupted = false;
        for (ProcessHandle handle : handles) {
            try {
                if (!handle.isAlive()) {
                    continue;
                }
                long remaining = deadline - System.nanoTime();
                if (remaining <= 0) {
                    continue;
                }
                handle.onExit().get(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException failure) {
                interrupted = true;
            } catch (ExecutionException | TimeoutException | RuntimeException ignored) {
                // Continue across the remaining captured process identities.
            }
        }
        return interrupted;
    }

    private static boolean containsUnsupportedShellSyntax(String command) {
        return command.contains("|")
                || command.contains("<")
                || command.contains(">")
                || command.contains("&&")
                || command.contains("||")
                || command.contains(";");
    }

    private static void closeQuietly(java.io.Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
            // Best-effort stream cleanup must not replace the command result.
        }
    }

    private static LiteFlowAgentContext requireContext(RuntimeContext runtimeContext) {
        if (runtimeContext == null) {
            throw new AgentConfigException("RuntimeContext is required for shell tools");
        }
        LiteFlowAgentContext context = runtimeContext.get(LiteFlowAgentContext.class);
        if (context == null) {
            throw new AgentConfigException("LiteFlowAgentContext is required for shell tools");
        }
        return context;
    }

    private static String safeMessage(Throwable failure) {
        String message = failure.getMessage();
        return (message == null ? failure.getClass().getSimpleName() : message)
                .replace("\"", "'");
    }

    private enum WaitResult {
        ACTIVE,
        EXITED,
        CANCELLED,
        INVOCATION_DEADLINE,
        TOOL_TIMEOUT
    }

    private record WaitWindow(WaitResult result, long waitNanos) {
    }

    private record OutputWait(WaitResult result, OutputCapture capture) {
    }

    private static final class ManagedProcessTree {
        private final ProcessHandle parent;
        private final Set<ProcessHandle> descendants = new LinkedHashSet<>();

        private ManagedProcessTree(ProcessHandle parent) {
            this.parent = parent;
        }

        private void captureDescendantsWhileParentAlive() {
            try {
                if (parent.isAlive()) {
                    try (var currentDescendants = parent.descendants()) {
                        currentDescendants.forEach(descendants::add);
                    }
                }
            } catch (RuntimeException ignored) {
                // A later bounded slice can retry while the original parent remains alive.
            }
        }

        private List<ProcessHandle> capturedChildFirst() {
            List<ProcessHandle> handles = new ArrayList<>(descendants.size() + 1);
            List<ProcessHandle> captured = List.copyOf(descendants);
            for (int index = captured.size() - 1; index >= 0; index--) {
                handles.add(captured.get(index));
            }
            handles.add(parent);
            return handles;
        }
    }

    private record OutputCapture(String content, boolean truncated) {
        private String render(int maximum) {
            if (!truncated) {
                return content;
            }
            String separator = content.endsWith("\n") || content.isEmpty() ? "" : "\n";
            return content + separator + "...[truncated after " + maximum + " bytes]";
        }
    }
}
