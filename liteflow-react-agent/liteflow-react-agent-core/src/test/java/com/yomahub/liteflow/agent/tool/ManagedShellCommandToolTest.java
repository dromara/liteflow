package com.yomahub.liteflow.agent.tool;

import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.ShellConfig;
import com.yomahub.liteflow.property.agent.ShellMode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ManagedShellCommandToolTest {

    @TempDir Path tmp;

    private AgentConfig whitelist(List<String> wl) {
        AgentConfig c = new AgentConfig();
        ShellConfig s = new ShellConfig();
        s.setMode(ShellMode.WHITELIST);
        s.setWhitelist(wl);
        s.setTimeout(Duration.ofSeconds(5));
        s.setMaxOutputBytes(4096);
        c.setShell(s);
        return c;
    }

    private AgentConfig blacklist(List<String> bl) {
        AgentConfig c = whitelist(List.of());
        c.getShell().setMode(ShellMode.BLACKLIST);
        c.getShell().setBlacklist(bl);
        return c;
    }

    private AgentConfig disabled() {
        AgentConfig c = whitelist(List.of());
        c.getShell().setMode(ShellMode.DISABLED);
        return c;
    }

    @Test
    void disabled_mode_rejects_all() {
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, disabled());
        String out = t.executeCommand("ls");
        assertTrue(out.contains("denied"), "DISABLED must reject: " + out);
    }

    @Test
    void whitelist_allows_only_listed() throws IOException {
        Files.writeString(tmp.resolve("hi.txt"), "x");
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, whitelist(List.of("ls")));
        String ok = t.executeCommand("ls");
        assertTrue(ok.contains("hi.txt"));

        String bad = t.executeCommand("rm hi.txt");
        assertTrue(bad.contains("not allowed"), "rm not in whitelist should be rejected: " + bad);
    }

    @Test
    void blacklist_blocks_listed() {
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, blacklist(List.of("rm", "sudo")));
        String bad = t.executeCommand("sudo rm -rf /");
        assertTrue(bad.contains("not allowed"));
    }

    @Test
    void execution_happens_in_workspace() throws IOException {
        Files.writeString(tmp.resolve("marker"), "");
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, whitelist(List.of("ls")));
        String out = t.executeCommand("ls");
        assertTrue(out.contains("marker"), "command must execute in workspace: " + out);
    }

    @Test
    void first_token_parsing_resists_combo() {
        ManagedShellCommandTool t = new ManagedShellCommandTool(tmp, whitelist(List.of("ls")));
        String out = t.executeCommand("cd /tmp && rm -rf /");
        assertTrue(out.contains("not allowed"), "combo command with non-whitelisted first token must be rejected: " + out);
    }
}
