package com.yomahub.liteflow.property.agent;

/**
 * Agent 工作区配置，对应配置段 {@code liteflow.agent.workspace.*}。
 *
 * <p>core 路径下工作区是 AgentScope 内置文件与 Shell 工具的根目录（baseDir）：
 * 工具的所有路径都被限制在该目录内，根目录在同一组件的所有会话间共享。
 * harness 路径下该根目录交给受控文件系统按会话隔离，{@link #autoCreate} 与
 * {@link #maxFileBytes} 仅由 harness 的 GUARDED_LOCAL 文件系统消费。
 */
public class WorkspaceConfig {

	/** Backend boundary used for the workspace. */
	private WorkspaceBackend backend = WorkspaceBackend.GUARDED_LOCAL;

	/** Whether the guarded local backend is explicitly trusted for local use. */
	private boolean trustedLocal;

    /**
     * 工作区根目录（必填）。
     *
     * <p>启用内置文件 / Shell 工具时会自动创建该目录，所有工具路径
     * 都被限制在其内，防止路径逃逸。
     */
    private String root;

    /**
     * 是否在初始化时自动创建 {@link #root} 目录。
     *
     * <p>由 harness 的 GUARDED_LOCAL 文件系统消费；关闭后需保证目录已存在。
     */
    private boolean autoCreate = true;

    /**
     * 单个文件可读写的最大字节数。
     *
     * <p>由 harness 的 GUARDED_LOCAL 文件系统在读写时校验，超出会拒绝操作。
     */
    private long maxFileBytes = 10L * 1024 * 1024;

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

    public long getMaxFileBytes() {
        return maxFileBytes;
    }

    public void setMaxFileBytes(long v) {
        this.maxFileBytes = v;
    }
}
