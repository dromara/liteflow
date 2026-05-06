package com.yomahub.liteflow.property.agent;

/**
 * 仅在 {@link MemoryStorageMode#REDIS} 模式下生效的配置项，对应配置段
 * {@code liteflow.agent.session.memory.redis.*}。
 *
 * <p>Redis 连接由用户自行创建并注册到框架容器中（Spring、Solon 等），
 * LiteFlow 通过 {@code ContextAware} 按 {@link #beanName} 查找。
 */
public class RedisMemoryConfig {

    /**
     * 用于查找 Redis 客户端 Bean 的名称（必填）。
     *
     * <p>{@code RedisAgentSessionFactory} 启动时通过 ContextAware 拿到该 bean，
     * 类型必须与 {@link #clientType} 匹配，否则会抛出 {@code AgentConfigException}。
     */
    private String beanName;

    /**
     * 已注册 Redis 客户端的类型，决定 AgentScope RedisSession 通过哪种方式适配。
     *
     * <p>{@code RedisAgentSessionFactory} 据此选择 {@code redissonClient} /
     * {@code jedisClient} / {@code lettuceClient} 三种构造方法之一进行反射注入。
     */
    private RedisClientType clientType = RedisClientType.REDISSON;

    /**
     * Redis 中存放 agent 会话数据使用的 key 前缀。
     *
     * <p>多业务、多环境共用同一个 Redis 实例时可通过该前缀做隔离，避免冲突。
     */
    private String keyPrefix = "liteflow:agent:session";

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    public RedisClientType getClientType() {
        return clientType;
    }

    public void setClientType(RedisClientType clientType) {
        this.clientType = clientType;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    /**
     * Redis 客户端类型枚举，每一项对应 AgentScope RedisSession 支持的一种客户端实现。
     */
    public enum RedisClientType {

        /** Redisson 客户端。 */
        REDISSON,

        /** Jedis 客户端（{@code redis.clients.jedis.UnifiedJedis}）。 */
        JEDIS,

        /** Lettuce 客户端（{@code io.lettuce.core.RedisClient}）。 */
        LETTUCE
    }
}
