package com.yomahub.liteflow.property.agent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Agent Shell 工具的安全配置，对应配置段 {@code liteflow.agent.shell.*}。
 *
 * <p>core 路径的命令过滤由 AgentScope 内置 {@code ShellCommandTool} 的
 * {@code UnixCommandValidator} 执行：仅白名单模式，匹配命令首 token，
 * 并拒绝包含 {@code & | ;} 或换行的链式命令；{@link #timeout} 作为
 * harness 路径下 Shell 命令的服务端超时上限。
 */
public class ShellConfig {

    /**
     * 命令过滤模式：白名单 / 关闭。
     *
     * <p>组件开启 shell 工具时必须为 {@link ShellMode#WHITELIST}，
     * {@link ShellMode#DISABLED} 会在构建期直接报错。
     */
	private ShellMode mode = ShellMode.DISABLED;

    /**
     * 白名单模式下允许执行的命令列表（仅匹配命令第一段）。
     *
     * <p>默认覆盖常用的只读 / 数据处理类命令，避免敏感操作；可按需追加。
     * 注意留空等价于放行全部命令，请始终保持非空。
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
     * 单条 shell 命令的最大执行时长（harness 路径消费）。
     *
     * <p>到时仍未结束会强制终止命令并返回超时提示，避免 agent 卡死；
     * core 路径使用 AgentScope 内置工具的按调用 timeout 参数（默认 300 秒）。
     */
    private Duration timeout = Duration.ofSeconds(30);

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

    public Duration getTimeout() {
        return timeout;
    }

    public void setTimeout(Duration v) {
        this.timeout = v;
    }
}
