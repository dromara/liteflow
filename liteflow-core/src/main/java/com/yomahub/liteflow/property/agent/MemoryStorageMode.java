package com.yomahub.liteflow.property.agent;

/**
 * Storage backend used to persist a ReActAgent's memory across executions
 * within a session.
 *
 * <ul>
 *   <li>{@link #NONE} – do not persist or even hold memory; equivalent to a stateless agent</li>
 *   <li>{@link #JVM} – keep memory in JVM heap only (default; behaviour identical to pre-2.15.4 releases)</li>
 *   <li>{@link #WORKSPACE_FILE} – persist memory as JSON files under each session's workspace directory
 *       using AgentScope's JsonSession</li>
 *   <li>{@link #REDIS} – persist memory through AgentScope's RedisSession; requires the user to
 *       provide a {@code RedissonClient} / {@code UnifiedJedis} / {@code RedisClient} bean</li>
 *   <li>{@link #MYSQL} – persist memory through AgentScope's MysqlSession; requires the user to
 *       provide a {@code javax.sql.DataSource} bean</li>
 * </ul>
 */
public enum MemoryStorageMode {
    NONE,
    JVM,
    WORKSPACE_FILE,
    REDIS,
    MYSQL
}
