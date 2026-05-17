package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ManagedShellCommandTool {

    private final Path workspace;
    private final ShellConfig shell;

    public ManagedShellCommandTool(Path workspace, AgentConfig cfg) {
        this.workspace = workspace.toAbsolutePath().normalize();
        this.shell = cfg.getShell();
    }

    @Tool(name = "execute_shell_command",
          description = "Execute a controlled shell command in the current workspace. Path traversal and blacklisted commands are blocked.")
    public String executeCommand(
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
            Future<String> outputFuture = outputReader.submit(() -> readLimited(p.getInputStream(), shell.getMaxOutputBytes()));
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
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return "{\"error\":\"" + e.getMessage().replace("\"", "'") + "\"}";
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
        List<byte[]> chunks = new ArrayList<>();
        long total = 0;
        int n;
        while ((n = in.read(buf)) > 0 && total < max) {
            int toCopy = (int) Math.min(n, max - total);
            byte[] c = new byte[toCopy];
            System.arraycopy(buf, 0, c, 0, toCopy);
            chunks.add(c);
            total += toCopy;
        }
        byte[] all = new byte[(int) total];
        int pos = 0;
        for (byte[] c : chunks) { System.arraycopy(c, 0, all, pos, c.length); pos += c.length; }
        return new String(all, StandardCharsets.UTF_8);
    }
}
