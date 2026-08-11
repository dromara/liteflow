package com.yomahub.liteflow.property.agent;

/**
 * 1.x Redis memory 配置的绑定兼容对象。
 *
 * <p>AgentScope 2 运行时不再反射适配 Redis 客户端。请由应用提供实现
 * {@code AgentStateStore} 的 Bean，并配置
 * {@code liteflow.agent.state-store.type=BEAN} 与
 * {@code liteflow.agent.state-store.bean-name}。
 */
public class RedisMemoryConfig {

	private boolean explicitlyConfigured;

    /**
     * 用于查找 Redis 客户端 Bean 的名称（必填）。
     *
     * <p>仅供旧配置迁移；2.0 运行时使用 {@code state-store.bean-name}。
     */
    private String beanName;

    /**
     * 旧 Redis 客户端类型。2.0 运行时不再按客户端类型做反射适配。
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
		this.explicitlyConfigured = true;
        this.beanName = beanName;
    }

    public RedisClientType getClientType() {
        return clientType;
    }

    public void setClientType(RedisClientType clientType) {
		this.explicitlyConfigured = true;
        this.clientType = clientType;
    }

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
		this.explicitlyConfigured = true;
        this.keyPrefix = keyPrefix;
    }

	boolean isExplicitlyConfigured() {
		return explicitlyConfigured;
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
