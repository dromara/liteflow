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
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ManagedShellCommandTool {

    private final GuardedWorkspacePathResolver resolver;
    private final ShellConfig shell;

    public ManagedShellCommandTool(GuardedWorkspacePathResolver resolver, AgentConfig cfg) {
        this.resolver = java.util.Objects.requireNonNull(resolver, "resolver");
        this.shell = cfg.getShell();
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
        try {
            ProcessBuilder pb = new ProcessBuilder(Arrays.asList(tokens));
            pb.directory(workspace.toFile());
            pb.redirectErrorStream(true);
            Process p = pb.start();
            closeQuietly(p.getOutputStream());
            ExecutorService outputReader = Executors.newSingleThreadExecutor(r -> {
                Thread t = new Thread(r, "liteflow-agent-shell-output-reader");
                t.setDaemon(true);
                return t;
            });
            Future<String> outputFuture = outputReader.submit(
                    () -> readLimited(p.getInputStream(), shell.getMaxOutputBytes()));
            try {
                boolean done = p.waitFor(shell.getTimeout().toMillis(), TimeUnit.MILLISECONDS);
                if (!done) {
                    p.destroyForcibly();
                    closeQuietly(p.getInputStream());
                    outputFuture.cancel(true);
                    return "{\"error\":\"timeout after " + shell.getTimeout().toMillis() + "ms\"}";
                }
                return outputFuture.get(1, TimeUnit.SECONDS);
            } catch (ExecutionException e) {
                Throwable cause = e.getCause();
                return "{\"error\":\"" + (cause == null ? e.getMessage() : cause.getMessage()).replace("\"", "'") + "\"}";
            } catch (TimeoutException e) {
                outputFuture.cancel(true);
                return "{\"error\":\"output read timeout\"}";
            } finally {
                outputReader.shutdownNow();
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return "{\"error\":\"" + safeMessage(e) + "\"}";
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
            // ignore close failures while cleaning up process streams
        }
    }

    private static String readLimited(InputStream in, long max) throws IOException {
        byte[] buf = new byte[4096];
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        long remaining = Math.max(0, max);
        while (remaining > 0) {
            int request = (int) Math.min(buf.length, remaining);
            int read = in.read(buf, 0, request);
            if (read < 0) {
                break;
            }
            output.write(buf, 0, read);
            remaining -= read;
        }
        return output.toString(StandardCharsets.UTF_8);
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
}
