package com.yomahub.liteflow.agent.harness.tool;

import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import io.agentscope.core.agent.RuntimeContext;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.tool.coding.CommandValidator;
import io.agentscope.core.tool.coding.UnixCommandValidator;
import io.agentscope.core.tool.coding.WindowsCommandValidator;
import io.agentscope.harness.agent.filesystem.AbstractFilesystem;
import io.agentscope.harness.agent.filesystem.sandbox.AbstractSandboxFilesystem;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/** Executes exclusively through the selected backend, with no fallback to the core host tool. */
public final class SandboxShellTool {
    private final AbstractSandboxFilesystem sandbox;
    private final Set<String> whitelist;
    private final CommandValidator validator;
    private final int maxTimeoutSeconds;
    private final boolean windows;

    public SandboxShellTool(AbstractSandboxFilesystem sandbox, ShellConfig config, boolean docker) {
        this.sandbox = Objects.requireNonNull(sandbox, "sandbox");
        Objects.requireNonNull(config, "config");
        if (config.getMode() != ShellMode.WHITELIST || config.getWhitelist() == null
                || config.getWhitelist().isEmpty()
                || config.getWhitelist().stream().anyMatch(command -> command == null || command.isBlank())) {
            throw new IllegalArgumentException("Sandbox Shell requires WHITELIST mode and a non-empty command whitelist");
        }
        this.whitelist = Set.copyOf(config.getWhitelist());
        // Docker sandbox paths and images in this backend are POSIX, even on a Windows host.
        this.windows = !docker && System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win");
        this.validator = windows ? new WindowsCommandValidator() : new UnixCommandValidator();
        long seconds = Objects.requireNonNull(config.getTimeout(), "timeout").getSeconds();
        if (seconds < 1) throw new IllegalArgumentException("liteflow.agent.harness.shell.timeout must be at least one second");
        this.maxTimeoutSeconds = (int) Math.min(Integer.MAX_VALUE, seconds);
    }

    @Tool(name = "execute", description = "Execute an allowed command through the configured filesystem backend. "
            + "Uses the command whitelist and server timeout. Docker commands execute inside the container.")
    public String execute(
            RuntimeContext context,
            @ToolParam(name = "command", description = "Command to execute") String command,
            @ToolParam(name = "working_directory", description = "Directory relative to the sandbox workspace", required = false) String workingDirectory,
            @ToolParam(name = "timeout", description = "Timeout in seconds, capped by the server", required = false) Integer timeout) {
        if (command == null || command.isBlank()) return "Exit code: 1\nCommand must not be blank";
        var validation = validator.validate(command, whitelist);
        if (!validation.isAllowed()) return "Exit code: 1\nCommand rejected: " + validation.getReason();
        String effective = command;
        if (workingDirectory != null && !workingDirectory.isBlank()) {
            String directory = workingDirectory.strip().replace('\\', '/');
            try {
                AbstractFilesystem.validatePath(directory);
                if (directory.startsWith("/") || directory.startsWith("~") || directory.matches("^[A-Za-z]:.*")) {
                    throw new IllegalArgumentException("Absolute directory");
                }
            } catch (IllegalArgumentException invalid) {
                return "Exit code: 1\nworking_directory must be relative to the sandbox workspace";
            }
            effective = windows
                    ? "cd /d \"" + directory.replace("\"", "\"\"") + "\" && " + command
                    : "cd '" + directory.replace("'", "'\\''") + "' && " + command;
        }
        int seconds = timeout == null || timeout <= 0 ? maxTimeoutSeconds : Math.min(timeout, maxTimeoutSeconds);
        var result = sandbox.execute(context, effective, seconds);
        return "Exit code: " + result.exitCode() + "\n" + result.output()
                + (result.truncated() ? "\n(output was truncated)" : "");
    }
}
