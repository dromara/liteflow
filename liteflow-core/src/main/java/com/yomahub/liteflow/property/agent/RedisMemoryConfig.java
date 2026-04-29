package com.yomahub.liteflow.property.agent;

/**
 * Settings that only apply when {@link MemoryStorageMode#REDIS} is selected.
 *
 * <p>Connections are not created by LiteFlow. Users must provide a Redis client
 * bean (Redisson / Jedis / Lettuce) through the framework's {@code ContextAware}
 * (Spring bean lookup, Solon container, etc.).
 */
public class RedisMemoryConfig {
    /** Bean name of the Redis client to look up via ContextAware. */
    private String beanName;

    /** Type of the configured client; affects how AgentScope adapts it. */
    private RedisClientType clientType = RedisClientType.REDISSON;

    /** Key prefix used inside Redis. */
    private String keyPrefix = "liteflow:agent:session";

    public String getBeanName() { return beanName; }
    public void setBeanName(String beanName) { this.beanName = beanName; }
    public RedisClientType getClientType() { return clientType; }
    public void setClientType(RedisClientType clientType) { this.clientType = clientType; }
    public String getKeyPrefix() { return keyPrefix; }
    public void setKeyPrefix(String keyPrefix) { this.keyPrefix = keyPrefix; }

    public enum RedisClientType { REDISSON, JEDIS, LETTUCE }
}
