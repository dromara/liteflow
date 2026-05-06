package com.yomahub.liteflow.property.agent;

/**
 * 仅在 {@link MemoryStorageMode#WORKSPACE_FILE} 模式下生效的配置项，
 * 对应配置段 {@code liteflow.agent.session.memory.workspace.*}。
 *
 * <p>该模式下记忆持久化会落盘到每个会话工作区目录下的固定子目录（{@link #SUB_DIR}），
 * 该位置故意硬编码、不暴露为可配置项：一方面避免与工具产生的业务文件混在一起，
 * 另一方面保证不同会话之间结构一致。需要自定义存储位置的用户应通过 SPI
 * 注入自己的 {@code AgentSessionFactory} 实现。
 */
public class WorkspaceMemoryConfig {

    /** 会话 JSON 文件存放的固定子目录名（位于工作区根目录下）。 */
    public static final String SUB_DIR = ".agent-session";
}
