package com.yomahub.liteflow.property.agent;

/**
 * 1.x Redis memory 配置的绑定兼容对象，仅用于旧配置迁移诊断。
 *
 * <p>setter 只记录用户显式绑定了旧键，使 {@link AgentConfig#validateForExecution()}
 * 在运行前要求迁移；AgentScope 2 runtime 不读取这些字段。Redis 状态存储应由应用提供
 * {@code AgentStateStore} Bean，并使用 {@code state-store.type=BEAN}。
 */
public class RedisMemoryConfig {

	private boolean explicitlyConfigured;

    /**
     * 旧 Redis 客户端 Bean 名，仅保留用于配置绑定和迁移诊断。
     */
    private String beanName;

    /**
     * 旧 Redis 客户端类型，仅保留用于配置绑定和迁移诊断；2.0 runtime 不读取。
     */
    private RedisClientType clientType = RedisClientType.REDISSON;

    /**
     * 旧 Redis key 前缀，仅保留用于配置绑定和迁移诊断；2.0 runtime 不读取。
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
     * 旧 Redis 客户端类型枚举，仅用于绑定历史配置值。
     */
    public enum RedisClientType {

        /** 历史 Redisson 选项。 */
        REDISSON,

        /** 历史 Jedis 选项。 */
        JEDIS,

        /** 历史 Lettuce 选项。 */
        LETTUCE
    }
}
