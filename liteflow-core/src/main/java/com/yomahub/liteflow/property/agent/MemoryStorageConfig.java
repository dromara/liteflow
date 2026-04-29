package com.yomahub.liteflow.property.agent;

/**
 * Memory persistence settings for ReActAgent sessions.
 *
 * <p>This config is intentionally orthogonal to {@link SessionConfig} (which
 * controls JVM-side session caching, idle timeout, LRU eviction). Memory
 * storage decides <em>where</em> the agent's conversation history is durably
 * kept; session config decides <em>how long</em> a hot agent is held in memory.
 */
public class MemoryStorageConfig {
    /** Default is {@link MemoryStorageMode#JVM} so existing deployments behave unchanged. */
    private MemoryStorageMode mode = MemoryStorageMode.JVM;

    private WorkspaceMemoryConfig workspace = new WorkspaceMemoryConfig();
    private RedisMemoryConfig redis = new RedisMemoryConfig();
    private MysqlMemoryConfig mysql = new MysqlMemoryConfig();

    /** Whether to lazily load existing session state on first {@code process()}. */
    private boolean loadOnFirstUse = true;
    /** Whether to save session state after a successful {@code process()}. */
    private boolean saveAfterCall = true;
    /** Whether to save session state when {@code process()} throws. */
    private boolean saveOnError = true;

    public MemoryStorageMode getMode() { return mode; }
    public void setMode(MemoryStorageMode mode) { this.mode = mode; }
    public WorkspaceMemoryConfig getWorkspace() { return workspace; }
    public void setWorkspace(WorkspaceMemoryConfig workspace) { this.workspace = workspace; }
    public RedisMemoryConfig getRedis() { return redis; }
    public void setRedis(RedisMemoryConfig redis) { this.redis = redis; }
    public MysqlMemoryConfig getMysql() { return mysql; }
    public void setMysql(MysqlMemoryConfig mysql) { this.mysql = mysql; }
    public boolean isLoadOnFirstUse() { return loadOnFirstUse; }
    public void setLoadOnFirstUse(boolean loadOnFirstUse) { this.loadOnFirstUse = loadOnFirstUse; }
    public boolean isSaveAfterCall() { return saveAfterCall; }
    public void setSaveAfterCall(boolean saveAfterCall) { this.saveAfterCall = saveAfterCall; }
    public boolean isSaveOnError() { return saveOnError; }
    public void setSaveOnError(boolean saveOnError) { this.saveOnError = saveOnError; }
}
