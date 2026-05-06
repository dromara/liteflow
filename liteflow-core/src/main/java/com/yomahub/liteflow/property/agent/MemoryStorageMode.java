package com.yomahub.liteflow.property.agent;

/**
 * ReActAgent 在同一会话中跨多次执行所使用的记忆持久化后端。
 *
 * <ul>
 *   <li>{@link #NONE} – 不保留也不持久化任何记忆，等价于无状态 agent</li>
 *   <li>{@link #JVM} – 仅保留在 JVM 堆内（默认行为，与 2.15.4 之前版本一致）</li>
 *   <li>{@link #WORKSPACE_FILE} – 通过 AgentScope 的 JsonSession，把记忆以 JSON 文件
 *       形式持久化到每个会话工作区目录下</li>
 *   <li>{@link #REDIS} – 通过 AgentScope 的 RedisSession 持久化，
 *       需要用户提供 {@code RedissonClient} / {@code UnifiedJedis} / {@code RedisClient} bean</li>
 *   <li>{@link #MYSQL} – 通过 AgentScope 的 MysqlSession 持久化，
 *       需要用户提供 {@code javax.sql.DataSource} bean</li>
 * </ul>
 */
public enum MemoryStorageMode {

    /** 完全无状态：既不持久化也不在内存中保留对话历史。 */
    NONE,

    /** 仅 JVM 堆内缓存，进程重启即丢失；为默认值。 */
    JVM,

    /** 工作区文件持久化，每个会话一个 JSON 文件，便于排查与离线分析。 */
    WORKSPACE_FILE,

    /** Redis 持久化，适合多实例部署、高并发场景。 */
    REDIS,

    /** MySQL 持久化，适合长期归档、跨服务共享的场景。 */
    MYSQL
}
