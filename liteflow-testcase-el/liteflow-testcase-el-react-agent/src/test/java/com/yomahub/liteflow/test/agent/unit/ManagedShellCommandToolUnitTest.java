package com.yomahub.liteflow.test.agent.unit;

import com.yomahub.liteflow.agent.tool.ManagedShellCommandTool;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.io.IOException;

/**
 * 覆盖 guide §6.4 受管 Shell 工具的命令过滤、超时与输出截断。
 */
public class ManagedShellCommandToolUnitTest {

    @TempDir
    Path workspace;

    private static String canonical(Path p) {
        try {
            return p.toRealPath().toString();
        } catch (IOException e) {
            return p.toAbsolutePath().normalize().toString();
        }
    }

    private AgentConfig newConfig(ShellMode mode) {
        AgentConfig cfg = new AgentConfig();
        cfg.getWorkspace().setRoot(workspace.toString());
        cfg.getShell().setMode(mode);
        return cfg;
    }

    @Test
    public void testDisabledModeDeniesAllCommands() {
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, newConfig(ShellMode.DISABLED));
        String out = tool.executeCommand("pwd");
        Assertions.assertTrue(out.contains("shell execution denied by policy"));
    }

    @Test
    public void testEmptyCommandIsRejected() {
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, newConfig(ShellMode.BLACKLIST));
        Assertions.assertTrue(tool.executeCommand("").contains("empty command"));
        Assertions.assertTrue(tool.executeCommand("   ").contains("empty command"));
    }

    @Test
    public void testBlacklistFiltersFirstToken() {
        AgentConfig cfg = newConfig(ShellMode.BLACKLIST);
        cfg.getShell().setBlacklist(List.of("rm"));
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, cfg);
        Assertions.assertTrue(tool.executeCommand("rm -rf /").contains("not allowed by blacklist"));
        // pwd 不在 blacklist 中应该执行成功，并返回 workspace 路径
        String pwd = tool.executeCommand("pwd").trim();
        Assertions.assertEquals(canonical(workspace), pwd);
    }

    @Test
    public void testWhitelistOnlyAllowsListedFirstToken() {
        AgentConfig cfg = newConfig(ShellMode.WHITELIST);
        cfg.getShell().setWhitelist(List.of("pwd"));
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, cfg);
        Assertions.assertTrue(tool.executeCommand("echo hi").contains("not allowed by whitelist"));
        Assertions.assertEquals(canonical(workspace),
                tool.executeCommand("pwd").trim());
    }

    @Test
    public void testUnsupportedShellSyntaxIsRejectedBeforeExecution() {
        AgentConfig cfg = newConfig(ShellMode.WHITELIST);
        cfg.getShell().setWhitelist(List.of("python3", "echo"));
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, cfg);

        Assertions.assertTrue(tool.executeCommand("python3 - <<'PY'").contains("unsupported shell syntax"));
        Assertions.assertTrue(tool.executeCommand("echo hi | wc -c").contains("unsupported shell syntax"));
        Assertions.assertTrue(tool.executeCommand("echo hi && echo bye").contains("unsupported shell syntax"));
    }

    @Test
    public void testTimeoutKillsLongRunningCommand() {
        AgentConfig cfg = newConfig(ShellMode.WHITELIST);
        cfg.getShell().setWhitelist(List.of("yes"));
        cfg.getShell().setTimeout(Duration.ofMillis(200));
        cfg.getShell().setMaxOutputBytes(64L);
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, cfg);
        String out = tool.executeCommand("yes");
        Assertions.assertTrue(out.contains("timeout"), "should timeout, got: " + out);
    }

    @Test
    public void testCommandWaitingForStdinReturnsWithinShellTimeout() {
        AgentConfig cfg = newConfig(ShellMode.WHITELIST);
        cfg.getShell().setWhitelist(List.of("cat"));
        cfg.getShell().setTimeout(Duration.ofMillis(200));
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, cfg);

        String out = Assertions.assertTimeoutPreemptively(Duration.ofSeconds(2),
                () -> tool.executeCommand("cat"));
        Assertions.assertTrue(out.isBlank() || out.contains("timeout"), "should not hang, got: " + out);
    }

    @Test
    public void testMaxOutputBytesTruncatesOutput() {
        // 用 head -c 4 控制输出长度，并把 max-output-bytes 设为 2 来观察截断。
        AgentConfig cfg = newConfig(ShellMode.WHITELIST);
        cfg.getShell().setWhitelist(List.of("printf"));
        cfg.getShell().setMaxOutputBytes(2L);
        ManagedShellCommandTool tool = new ManagedShellCommandTool(workspace, cfg);
        String out = tool.executeCommand("printf abcdef");
        Assertions.assertTrue(out.length() <= 2, "max-output-bytes 应截断输出，实际=" + out);
    }
}
