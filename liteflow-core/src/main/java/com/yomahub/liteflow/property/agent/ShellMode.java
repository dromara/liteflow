package com.yomahub.liteflow.property.agent;

/**
 * Shell 工具的命令过滤模式，配合 {@link ShellConfig} 中的白名单使用。
 */
public enum ShellMode {

    /** 白名单模式：仅 {@link ShellConfig#getWhitelist()} 中列出的命令可执行（默认）。 */
    WHITELIST,

    /** 关闭模式：所有 shell 命令都被拒绝执行，等价于禁用 shell 工具。 */
    DISABLED
}
