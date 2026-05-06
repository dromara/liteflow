package com.yomahub.liteflow.property.agent;

/**
 * ReActAgent 会话记忆持久化设置，对应配置段
 * {@code liteflow.agent.session.memory.*}。
 *
 * <p>本配置与 {@link SessionConfig} 是正交的关注点：
 * {@link SessionConfig} 控制 JVM 内 agent 实例的缓存、空闲超时与 LRU 淘汰；
 * 而本配置决定 agent 的对话历史 <em>持久化到哪里</em>，例如 JVM 堆内、本地文件、
 * Redis 或 MySQL。
 */
public class MemoryStorageConfig {

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
     * <p>由 {@code AgentSessionManager} 在装载历史记忆时读取；为 false 时新 agent
     * 始终从空白记忆开始，不会回放历史。
     */
    private boolean loadOnFirstUse = true;

    /**
     * {@code process()} 成功执行后是否回写会话状态。
     *
     * <p>由 {@code ReActAgentComponent} 在执行结束（无异常）时读取，决定是否将本轮
     * 对话追加保存到所选存储后端。
     */
    private boolean saveAfterCall = true;

    /**
     * {@code process()} 抛出异常时是否仍回写会话状态。
     *
     * <p>由 {@code ReActAgentComponent} 在异常分支中读取；开启后即使本轮失败也会
     * 保留上下文以便后续诊断或重试。
     */
    private boolean saveOnError = true;

    public MemoryStorageMode getMode() {
        return mode;
    }

    public void setMode(MemoryStorageMode mode) {
        this.mode = mode;
    }

    public LocalFileMemoryConfig getLocalFile() {
        return localFile;
    }

    public void setLocalFile(LocalFileMemoryConfig localFile) {
        this.localFile = localFile;
    }

    public RedisMemoryConfig getRedis() {
        return redis;
    }

    public void setRedis(RedisMemoryConfig redis) {
        this.redis = redis;
    }

    public MysqlMemoryConfig getMysql() {
        return mysql;
    }

    public void setMysql(MysqlMemoryConfig mysql) {
        this.mysql = mysql;
    }

    public boolean isLoadOnFirstUse() {
        return loadOnFirstUse;
    }

    public void setLoadOnFirstUse(boolean loadOnFirstUse) {
        this.loadOnFirstUse = loadOnFirstUse;
    }

    public boolean isSaveAfterCall() {
        return saveAfterCall;
    }

    public void setSaveAfterCall(boolean saveAfterCall) {
        this.saveAfterCall = saveAfterCall;
    }

    public boolean isSaveOnError() {
        return saveOnError;
    }

    public void setSaveOnError(boolean saveOnError) {
        this.saveOnError = saveOnError;
    }
}
