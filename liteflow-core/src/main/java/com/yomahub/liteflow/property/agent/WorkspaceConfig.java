package com.yomahub.liteflow.property.agent;

/**
 * Agent 工作区配置，对应配置段 {@code liteflow.agent.workspace.*}。
 *
 * <p>AgentScope 2 通过受保护的路径解析器按运行时身份隔离目录，并由组件运行时
 * 持有相关资源；核心不会注册静态会话管理器或 JVM shutdown hook。
 */
public class WorkspaceConfig {

	/** Backend boundary used for the workspace. */
	private WorkspaceBackend backend = WorkspaceBackend.GUARDED_LOCAL;

	/** Whether the guarded local backend is explicitly trusted for local use. */
	private boolean trustedLocal;

    /**
     * 工作区根目录（必填）。
     *
     * <p>受保护的工作区路径解析器会规范化该路径，并按运行时身份创建子目录。
     */
    private String root;

    /**
     * 是否在启动时自动创建 {@link #root} 目录。
     *
     * <p>关闭后用户需保证目录已存在，否则工作区工具初始化会失败。
     */
    private boolean autoCreate = true;

    /**
     * 1.x 会话淘汰清理开关，仅为配置绑定兼容保留；AgentScope 2 核心不执行会话淘汰。
     */
    private boolean cleanupOnSessionExpire = true;

    /**
     * 1.x JVM 清理开关，仅为配置绑定兼容保留；AgentScope 2 核心不注册关停钩子。
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

	public WorkspaceBackend getBackend() {
		return backend;
	}

	public void setBackend(WorkspaceBackend backend) {
		this.backend = backend;
	}

	public boolean isTrustedLocal() {
		return trustedLocal;
	}

	public void setTrustedLocal(boolean trustedLocal) {
		this.trustedLocal = trustedLocal;
	}

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
