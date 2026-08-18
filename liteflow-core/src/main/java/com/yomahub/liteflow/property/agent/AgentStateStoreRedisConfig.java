package com.yomahub.liteflow.property.agent;

/**
 * Redis state-store settings, bound from {@code liteflow.agent.state-store.redis.*}.
 *
 * <p>Exactly one connection source must be configured: either {@code uri} (LiteFlow
 * builds and owns a Lettuce client) or {@code client-bean-name} (an existing
 * Jedis {@code UnifiedJedis}, Redisson {@code RedissonClient} or Lettuce
 * {@code RedisClient}/{@code RedisClusterClient} bean whose lifecycle stays with
 * the application).
 */
public class AgentStateStoreRedisConfig {

	/** Redis URI such as {@code redis://localhost:6379}; when set LiteFlow owns the client. */
	private String uri;

	/** Name of a container-managed Redis client bean; when set the application owns the client. */
	private String clientBeanName;

	/**
	 * Key prefix inside Redis.
	 *
	 * <p>Defaults to the agentscope extension prefix ({@code agentscope:session:})
	 * when left blank; session ids are additionally namespaced by LiteFlow itself.
	 */
	private String keyPrefix;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getClientBeanName() {
		return clientBeanName;
	}

	public void setClientBeanName(String clientBeanName) {
		this.clientBeanName = clientBeanName;
	}

	public String getKeyPrefix() {
		return keyPrefix;
	}

	public void setKeyPrefix(String keyPrefix) {
		this.keyPrefix = keyPrefix;
	}
}
