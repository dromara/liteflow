package com.yomahub.liteflow.property.agent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

public class ShellConfig {
    private ShellMode mode = ShellMode.WHITELIST;
    private List<String> whitelist = Arrays.asList(
            "ls", "cat", "grep", "find", "head", "tail", "wc", "sed", "awk", "python3", "node");
    private List<String> blacklist = Arrays.asList("rm", "sudo", "shutdown", "mkfs", "dd");
    private Duration timeout = Duration.ofSeconds(30);
    private long maxOutputBytes = 1024L * 1024;

    public ShellMode getMode() { return mode; }
    public void setMode(ShellMode v) { this.mode = v; }
    public List<String> getWhitelist() { return whitelist; }
    public void setWhitelist(List<String> v) { this.whitelist = v; }
    public List<String> getBlacklist() { return blacklist; }
    public void setBlacklist(List<String> v) { this.blacklist = v; }
    public Duration getTimeout() { return timeout; }
    public void setTimeout(Duration v) { this.timeout = v; }
    public long getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(long v) { this.maxOutputBytes = v; }
}
