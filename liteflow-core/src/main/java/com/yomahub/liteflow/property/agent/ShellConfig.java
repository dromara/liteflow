package com.yomahub.liteflow.property.agent;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * Harness Shell 工具的安全配置，对应配置段 {@code liteflow.agent.harness.shell.*}。
 *
 * <p>Harness 使用 AgentScope 平台命令校验器进行白名单校验，匹配命令首 token，
 * 并拒绝包含 {@code & | ;} 或换行的链式命令；{@link #timeout} 作为
 * 本地和 Docker Shell 命令的服务端超时上限。
 */
public class ShellConfig {

    /**
     * 命令过滤模式：白名单 / 关闭。
     *
     * <p>组件开启 shell 工具时必须为 {@link ShellMode#WHITELIST}，
     * {@link ShellMode#DISABLED} 会在构建期直接报错。
     */
	private ShellMode mode = ShellMode.WHITELIST;

    /**
     * 白名单模式下允许执行的命令列表（仅匹配命令第一段）。
     *
     * <p>省略配置时使用以下默认命令。用户配置的列表替换默认列表；显式空列表会被拒绝。
     * 默认包含脚本解释器，命令白名单不等同于操作系统隔离。
     */
    private List<String> whitelist = Arrays.asList(
            // Shell、脚本解释器与构建工具
            "sh", "bash", "python", "python3", "node", "git", "npm", "npx", "java", "javac", "mvn", "gradle",
            // 文件/目录操作、浏览与查找
            "mkdir", "cp", "mv", "rm", "touch", "chmod", "ls", "find", "tree", "stat", "file", "basename", "dirname", "pwd", "which",
            // 文件内容查看与文本处理
            "cat", "head", "tail", "grep", "sed", "awk", "wc", "sort", "uniq", "cut", "tr", "diff",
            // 文本/数值输出与计算
            "echo", "printf", "expr",
            // 系统/环境信息（只读）
            "date", "whoami", "hostname", "uname", "env", "df", "du", "ps",
            // 哈希与数据格式
            "md5sum", "sha256sum", "jq",
            // 网络请求
            "curl", "wget");

    /**
     * 单条 Shell 命令的最大执行时长，默认 1 分钟。
     *
     * <p>到时仍未结束会强制终止命令并返回超时提示；模型传入的 timeout 参数只能缩短该上限。
     */
    private Duration timeout = Duration.ofMinutes(1);

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
