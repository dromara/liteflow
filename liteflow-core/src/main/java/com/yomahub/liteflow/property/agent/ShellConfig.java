package com.yomahub.liteflow.property.agent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Agent 内置 Shell 工具的安全配置，对应配置段 {@code liteflow.agent.shell.*}。
 *
 * <p>所有字段都由 {@code ManagedShellCommandTool} 在执行命令前后读取，
 * 用于做命令过滤、超时杀死与输出截断，避免 LLM 触发危险或失控的系统调用。
 */
public class ShellConfig {

    /**
     * 命令过滤模式：白名单 / 黑名单 / 关闭。
     *
     * <p>{@code ManagedShellCommandTool} 取出待执行命令的第一个 token，根据该模式
     * 与 {@link #whitelist} / {@link #blacklist} 比对，决定是否放行。
     */
    private ShellMode mode = ShellMode.WHITELIST;

    /**
     * 白名单模式下允许执行的命令列表（仅匹配命令第一段）。
     *
     * <p>默认覆盖常用的只读 / 数据处理类命令，避免敏感操作；可按需追加。
     */
    private List<String> whitelist = Arrays.asList(
            // 文件/目录浏览与查找
            "ls", "find", "tree", "stat", "file", "basename", "dirname", "pwd", "which",
            // 文件内容查看与文本处理
            "cat", "head", "tail", "grep", "sed", "awk", "wc", "sort", "uniq", "cut", "tr", "diff",
            // 文本/数值输出与计算
            "echo", "printf", "expr",
            // 系统/环境信息（只读）
            "date", "whoami", "hostname", "uname", "env", "df", "du", "ps",
            // 哈希与数据格式
            "md5sum", "sha256sum", "jq",
            // 网络请求
            "curl", "wget",
            // 脚本解释器
            "python3", "node");

    /**
     * 黑名单模式下禁止执行的命令列表（仅匹配命令第一段）。
     *
     * <p>默认拦截删除、提权、关机、磁盘格式化等高风险命令。
     */
    private List<String> blacklist = Arrays.asList("rm", "sudo", "shutdown", "mkfs", "dd");

    /**
     * 单条 shell 命令的最大执行时长。
     *
     * <p>到时仍未结束，{@code ManagedShellCommandTool} 会强制终止子进程
     * 并返回带 timeout 提示的 JSON 结果，避免 agent 卡死。
     */
    private Duration timeout = Duration.ofSeconds(30);

    /**
     * 单次命令收集的最大输出字节数。
     *
     * <p>用于防止 LLM 在执行类似 {@code cat 大文件} 时把巨量内容塞回上下文，
     * 触发上限后会截断剩余 stdout。
     */
    private long maxOutputBytes = 1024L * 1024;

    public ShellMode getMode() {
        return mode;
    }

    public void setMode(ShellMode v) {
        this.mode = v;
    }

    public List<String> getWhitelist() {
        return whitelist;
    }

    public void setWhitelist(List<String> v) {
        this.whitelist = v;
    }

    public List<String> getBlacklist() {
        return blacklist;
    }

    public void setBlacklist(List<String> v) {
        this.blacklist = v;
    }

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration v) {
        this.timeout = v;
    }

    public long getMaxOutputBytes() {
        return maxOutputBytes;
    }

    public void setMaxOutputBytes(long v) {
        this.maxOutputBytes = v;
    }
}
