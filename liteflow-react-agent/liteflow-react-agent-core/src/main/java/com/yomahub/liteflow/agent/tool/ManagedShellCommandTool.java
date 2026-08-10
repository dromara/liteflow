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
import java.util.List;
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
    private static final long OUTPUT_JOIN_NANOS = TimeUnit.SECONDS.toNanos(1);
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
        ExecutorService outputReader = null;
        Future<OutputCapture> outputFuture = null;
        boolean exitedNormally = false;
        boolean interrupted = false;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(Arrays.asList(tokens));
            processBuilder.directory(workspace.toFile());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();
            closeQuietly(process.getOutputStream());
            Process runningProcess = process;
            outputReader = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "liteflow-agent-shell-output-reader");
                thread.setDaemon(true);
                return thread;
            });
            outputFuture = outputReader.submit(
                    () -> drainOutput(runningProcess.getInputStream(), maxOutputBytes));

            WaitResult waitResult = waitForProcess(process, context);
            if (waitResult != WaitResult.EXITED) {
                return switch (waitResult) {
                    case CANCELLED -> "{\"error\":\"shell execution cancelled\"}";
                    case INVOCATION_DEADLINE ->
                            "{\"error\":\"invocation deadline exceeded\"}";
                    case TOOL_TIMEOUT ->
                            "{\"error\":\"timeout after " + shell.getTimeout() + "\"}";
                    default -> throw new IllegalStateException("unexpected wait result");
                };
            }
            OutputCapture capture = outputFuture.get(OUTPUT_JOIN_NANOS, TimeUnit.NANOSECONDS);
            exitedNormally = true;
            return capture.render(maxOutputBytes);
        } catch (InterruptedException failure) {
            interrupted = true;
            return "{\"error\":\"shell execution interrupted\"}";
        } catch (ExecutionException failure) {
            Throwable cause = failure.getCause();
            return "{\"error\":\"" + safeMessage(cause == null ? failure : cause) + "\"}";
        } catch (TimeoutException failure) {
            return "{\"error\":\"output drain timeout\"}";
        } catch (IOException failure) {
            return "{\"error\":\"" + safeMessage(failure) + "\"}";
        } finally {
            if (process != null && !exitedNormally) {
                try {
                    terminateProcessTree(process);
                } catch (RuntimeException ignored) {
                    // Continue with stream and reader cleanup if ProcessHandle cleanup fails.
                }
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
                    outputReader.awaitTermination(
                            OUTPUT_JOIN_NANOS, TimeUnit.NANOSECONDS);
                } catch (InterruptedException failure) {
                    interrupted = true;
                }
            }
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private WaitResult waitForProcess(Process process, LiteFlowAgentContext context)
            throws InterruptedException {
        long started = System.nanoTime();
        while (true) {
            if (context.isCancelled()) {
                return WaitResult.CANCELLED;
            }
            Instant now = Instant.now();
            if (!now.isBefore(context.getDeadline())) {
                return WaitResult.INVOCATION_DEADLINE;
            }
            long remaining = timeoutNanos - (System.nanoTime() - started);
            if (remaining <= 0) {
                return WaitResult.TOOL_TIMEOUT;
            }
            long waitNanos = Math.min(WAIT_SLICE_NANOS, remaining);
            long invocationRemaining = nanosUntil(now, context.getDeadline());
            waitNanos = Math.min(waitNanos, invocationRemaining);
            if (waitNanos <= 0) {
                return WaitResult.INVOCATION_DEADLINE;
            }
            if (process.waitFor(waitNanos, TimeUnit.NANOSECONDS)) {
                return WaitResult.EXITED;
            }
        }
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

    private static void terminateProcessTree(Process process) {
        ProcessHandle parent = process.toHandle();
        List<ProcessHandle> descendants = new ArrayList<>(parent.descendants().toList());
        for (int index = descendants.size() - 1; index >= 0; index--) {
            descendants.get(index).destroy();
        }
        parent.destroy();
        awaitExit(descendants, parent, PROCESS_CLEANUP_NANOS / 2);
        for (int index = descendants.size() - 1; index >= 0; index--) {
            ProcessHandle descendant = descendants.get(index);
            if (descendant.isAlive()) {
                descendant.destroyForcibly();
            }
        }
        if (parent.isAlive()) {
            parent.destroyForcibly();
        }
        awaitExit(descendants, parent, PROCESS_CLEANUP_NANOS / 2);
    }

    private static void awaitExit(
            List<ProcessHandle> descendants, ProcessHandle parent, long budgetNanos) {
        long deadline = System.nanoTime() + budgetNanos;
        for (ProcessHandle handle : descendants) {
            awaitExit(handle, deadline);
        }
        awaitExit(parent, deadline);
    }

    private static void awaitExit(ProcessHandle handle, long deadline) {
        if (!handle.isAlive()) {
            return;
        }
        long remaining = deadline - System.nanoTime();
        if (remaining <= 0) {
            return;
        }
        try {
            handle.onExit().get(remaining, TimeUnit.NANOSECONDS);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        } catch (ExecutionException | TimeoutException ignored) {
            // The second cleanup phase escalates to destroyForcibly within the shared budget.
        }
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
        EXITED,
        CANCELLED,
        INVOCATION_DEADLINE,
        TOOL_TIMEOUT
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
