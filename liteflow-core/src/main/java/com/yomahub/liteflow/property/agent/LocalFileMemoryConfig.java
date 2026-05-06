package com.yomahub.liteflow.property.agent;

/**
 * 仅在 {@link MemoryStorageMode#LOCAL_FILE} 模式下生效的配置项，
 * 对应配置段 {@code liteflow.agent.session.memory.local-file.*}。
 *
 * <p>该模式下记忆持久化会落盘到 {@code workspace.root} 之下的
 * {@link #SUB_DIR} 子目录中，按 {@code sessionId} 再分一层；该目录与
 * 各 session 自己的 workspace（{@code workspace.root/<sessionId>/}）平级而不嵌套，
 * 这样可以避免内置 {@code WorkspaceFileTools}
 * 读到或覆盖 agent 自己的记忆，也让 {@code cleanup-on-session-expire}
 * 在清空 workspace 子目录时不会误删持久化的记忆。
 *
 * <p>该位置故意硬编码、不暴露为可配置项：保证不同会话之间结构一致，
 * 且与 Redis、MySQL 等远程后端的"持久化与 workspace 生命周期解耦"语义对齐。
 * 需要自定义存储位置的用户应通过 SPI 注入自己的
 * {@code AgentSessionFactory} 实现。
 */
public class LocalFileMemoryConfig {

    /** 会话 JSON 文件存放的固定子目录名，位于 {@code workspace.root} 之下。 */
    public static final String SUB_DIR = ".agent-session";
}
