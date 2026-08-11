package com.yomahub.liteflow.property.agent;

/**
 * AgentScope 1 会话记忆持久化兼容设置，对应旧配置段
 * {@code liteflow.agent.session.memory.*}。
 *
 * <p>AgentScope 2 不读取这些存储选项；setter 仅记录用户显式使用了旧配置，
 * 以便 {@link AgentConfig} 在执行前给出迁移到 {@code state-store.*} 的诊断。
 */
public class MemoryStorageConfig {

	/*
	 * Setters are the property-binding boundary. The flag intentionally distinguishes an
	 * untouched default from an explicit legacy session.memory.* configuration.
	 */
	private boolean explicitlyConfigured;

    /**
     * 记忆存储后端。
     *
     * <p>默认值为 {@link MemoryStorageMode#JVM}，与 2.15.4 之前版本的行为保持一致，
     * 保证已有部署在升级时无感知。
     */
    private MemoryStorageMode mode = MemoryStorageMode.JVM;

    /** {@link MemoryStorageMode#LOCAL_FILE} 模式下生效的子配置。 */
    private LocalFileMemoryConfig localFile = new LocalFileMemoryConfig();

    /** {@link MemoryStorageMode#REDIS} 模式下生效的子配置。 */
    private RedisMemoryConfig redis = new RedisMemoryConfig();

    /** {@link MemoryStorageMode#MYSQL} 模式下生效的子配置。 */
    private MysqlMemoryConfig mysql = new MysqlMemoryConfig();

    /**
     * 是否在首次 {@code process()} 调用时延迟加载已存在的会话状态。
     *
     * <p>仅为 1.x 配置绑定兼容保留。
     */
    private boolean loadOnFirstUse = true;

    /**
     * {@code process()} 成功执行后是否回写会话状态。
     *
     * <p>仅为 1.x 配置绑定兼容保留。
     */
    private boolean saveAfterCall = true;

    /**
     * {@code process()} 抛出异常时是否仍回写会话状态。
     *
     * <p>仅为 1.x 配置绑定兼容保留。
     */
    private boolean saveOnError = true;

    public MemoryStorageMode getMode() {
        return mode;
    }

	public void setMode(MemoryStorageMode mode) {
		markExplicitlyConfigured();
		this.mode = mode;
    }

    public LocalFileMemoryConfig getLocalFile() {
        return localFile;
    }

	public void setLocalFile(LocalFileMemoryConfig localFile) {
		markExplicitlyConfigured();
		this.localFile = localFile;
    }

    public RedisMemoryConfig getRedis() {
        return redis;
    }

	public void setRedis(RedisMemoryConfig redis) {
		markExplicitlyConfigured();
		this.redis = redis;
    }

    public MysqlMemoryConfig getMysql() {
        return mysql;
    }

	public void setMysql(MysqlMemoryConfig mysql) {
		markExplicitlyConfigured();
		this.mysql = mysql;
    }

    public boolean isLoadOnFirstUse() {
        return loadOnFirstUse;
    }

	public void setLoadOnFirstUse(boolean loadOnFirstUse) {
		markExplicitlyConfigured();
		this.loadOnFirstUse = loadOnFirstUse;
    }

    public boolean isSaveAfterCall() {
        return saveAfterCall;
    }

	public void setSaveAfterCall(boolean saveAfterCall) {
		markExplicitlyConfigured();
		this.saveAfterCall = saveAfterCall;
    }

    public boolean isSaveOnError() {
        return saveOnError;
    }

	public void setSaveOnError(boolean saveOnError) {
		markExplicitlyConfigured();
		this.saveOnError = saveOnError;
	}

	boolean isExplicitlyConfigured() {
		return explicitlyConfigured || (redis != null && redis.isExplicitlyConfigured())
				|| (mysql != null && mysql.isExplicitlyConfigured());
	}

	void markExplicitlyConfigured() {
		this.explicitlyConfigured = true;
	}
}
