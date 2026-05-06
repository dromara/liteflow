package com.yomahub.liteflow.property.agent;

/**
 * Agent 工作区配置，对应配置段 {@code liteflow.agent.workspace.*}。
 *
 * <p>工作区是每个会话独立的本地目录，agent 可在其中读写文件、执行 shell 命令。
 * 各字段会在 {@code AgentSessionManager}（管理目录生命周期）和
 * {@code WorkspaceFileTools}（文件读写工具）中分别使用。
 */
public class WorkspaceConfig {

    /**
     * 工作区根目录（必填）。
     *
     * <p>{@code AgentSessionManager} 启动时会规范化该路径，并按会话维度在其下创建
     * 子目录；为空时会跳过工作区相关初始化（含会话清理任务）。
     */
    private String root;

    /**
     * 是否在启动时自动创建 {@link #root} 目录。
     *
     * <p>{@code AgentSessionManager} 据此调用 {@code Files.createDirectories(...)}，
     * 关闭后用户需保证目录已存在，否则会话写入会失败。
     */
    private boolean autoCreate = true;

    /**
     * 会话超时被淘汰时是否同时清理其工作区目录。
     *
     * <p>{@code AgentSessionManager} 在 LRU / 空闲淘汰一个会话时根据该开关
     * 决定是否递归删除目录，关闭后历史文件会保留供事后排查。
     */
    private boolean cleanupOnSessionExpire = true;

    /**
     * JVM 关闭时是否清理整个工作区根目录。
     *
     * <p>{@code AgentSessionManager} 会在该开关打开时注册关停钩子；默认关闭，
     * 避免因进程异常退出而误删用户的持久化数据。
     */
    private boolean cleanupOnJvmShutdown = false;

    /**
     * 单个文件可读写的最大字节数。
     *
     * <p>{@code WorkspaceFileTools} 在文件读写时按该上限校验，超出会拒绝操作，
     * 用于防止 LLM 通过读写超大文件耗尽内存或上下文。
     */
    private long maxFileBytes = 10L * 1024 * 1024;

    /**
     * 列目录类工具一次最多返回的条目数。
     *
     * <p>{@code WorkspaceFileTools} 在执行列出工作区文件等操作时使用，避免
     * 大目录返回过多条目导致 LLM 上下文被塞满。
     */
    private int maxListSize = 1000;

    public String getRoot() {
        return root;
    }

    public void setRoot(String root) {
        this.root = root;
    }

    public boolean isAutoCreate() {
        return autoCreate;
    }

    public void setAutoCreate(boolean autoCreate) {
        this.autoCreate = autoCreate;
    }

    public boolean isCleanupOnSessionExpire() {
        return cleanupOnSessionExpire;
    }

    public void setCleanupOnSessionExpire(boolean v) {
        this.cleanupOnSessionExpire = v;
    }

    public boolean isCleanupOnJvmShutdown() {
        return cleanupOnJvmShutdown;
    }

    public void setCleanupOnJvmShutdown(boolean v) {
        this.cleanupOnJvmShutdown = v;
    }

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long v) {
        this.maxFileBytes = v;
    }

    public int getMaxListSize() {
        return maxListSize;
    }

    public void setMaxListSize(int v) {
        this.maxListSize = v;
    }
}
