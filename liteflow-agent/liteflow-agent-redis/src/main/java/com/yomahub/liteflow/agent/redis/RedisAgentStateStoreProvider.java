package com.yomahub.liteflow.agent.redis;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.agent.state.AgentStateStoreProvider;
import com.yomahub.liteflow.agent.state.ResolvedAgentStateStore;
import com.yomahub.liteflow.property.agent.AgentStateStoreConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreRedisConfig;
import com.yomahub.liteflow.property.agent.AgentStateStoreType;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.agentscope.extensions.redis.state.RedisAgentStateStore;
import io.agentscope.extensions.redis.state.RedisClientAdapter;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.cluster.RedisClusterClient;
import org.redisson.api.RedissonClient;
import redis.clients.jedis.UnifiedJedis;

import java.util.Objects;
import java.util.function.Function;

/**
 * Builds the Redis {@link AgentStateStoreType#REDIS} backend on top of the official
 * {@code agentscope-extensions-redis} {@link RedisAgentStateStore}.
 *
 * <p>Connection source is either {@code liteflow.agent.state-store.redis.uri}
 * (LiteFlow builds and owns a Lettuce client) or
 * {@code liteflow.agent.state-store.redis.client-bean-name} (an existing Jedis /
 * Lettuce / Redisson client bean whose lifecycle stays with the application).
 */
public final class RedisAgentStateStoreProvider implements AgentStateStoreProvider {

	private static final String BEAN_TYPE_ERROR = "state-store.redis.client-bean-name must resolve to a "
			+ "redis.clients.jedis.UnifiedJedis, io.lettuce.core.RedisClient, "
			+ "io.lettuce.core.cluster.RedisClusterClient, org.redisson.api.RedissonClient "
			+ "or io.agentscope.extensions.redis.state.RedisClientAdapter";

	private final Function<String, Object> beanLookup;

	public RedisAgentStateStoreProvider() {
		this(name -> ContextAwareHolder.loadContextAware().getBean(name));
	}

	public RedisAgentStateStoreProvider(Function<String, Object> beanLookup) {
		this.beanLookup = Objects.requireNonNull(beanLookup, "beanLookup");
	}

	@Override
	public AgentStateStoreType type() {
		return AgentStateStoreType.REDIS;
	}

	@Override
	public ResolvedAgentStateStore resolve(AgentStateStoreConfig config) {
		AgentStateStoreRedisConfig redis = config.getRedis();
		if (redis == null) {
			throw new AgentConfigException("liteflow.agent.state-store.redis must not be null");
		}
		String uri = trimToNull(redis.getUri());
		String beanName = trimToNull(redis.getClientBeanName());
		if (uri != null && beanName != null) {
			throw new AgentConfigException(
					"state-store.redis.uri and state-store.redis.client-bean-name are mutually exclusive");
		}
		if (uri == null && beanName == null) {
			throw new AgentConfigException(
					"state-store type REDIS requires either state-store.redis.uri "
							+ "or state-store.redis.client-bean-name");
		}
		return beanName != null ? resolveFromBean(redis, beanName) : resolveFromUri(redis, uri);
	}

	private ResolvedAgentStateStore resolveFromBean(
			AgentStateStoreRedisConfig redis, String beanName) {
		Object client;
		try {
			client = beanLookup.apply(beanName);
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"Redis client bean '" + beanName + "' could not be resolved", failure);
		}
		if (client == null) {
			throw new AgentConfigException("Redis client bean '" + beanName + "' was not found");
		}
		RedisAgentStateStore.Builder builder = RedisAgentStateStore.builder();
		if (client instanceof UnifiedJedis unifiedJedis) {
			builder.jedisClient(unifiedJedis);
		} else if (client instanceof RedissonClient redisson) {
			builder.redissonClient(redisson);
		} else if (client instanceof RedisClusterClient clusterClient) {
			builder.lettuceClusterClient(clusterClient);
		} else if (client instanceof RedisClient lettuceClient) {
			builder.lettuceClient(lettuceClient);
		} else if (client instanceof RedisClientAdapter adapter) {
			builder.clientAdapter(adapter);
		} else {
			throw new AgentConfigException(BEAN_TYPE_ERROR + ", got: " + client.getClass().getName());
		}
		return new ResolvedAgentStateStore(applyKeyPrefix(builder, redis).build(), false);
	}

	private ResolvedAgentStateStore resolveFromUri(AgentStateStoreRedisConfig redis, String uri) {
		RedisClient client;
		try {
			client = RedisClient.create(RedisURI.create(uri));
		} catch (RuntimeException | LinkageError failure) {
			throw new AgentConfigException(
					"state-store.redis.uri is invalid: " + uri, failure);
		}
		try {
			RedisAgentStateStore store = applyKeyPrefix(
					RedisAgentStateStore.builder().lettuceClient(client), redis).build();
			return new ResolvedAgentStateStore(store, true);
		} catch (RuntimeException | LinkageError failure) {
			client.shutdown();
			throw new AgentConfigException("Redis state store could not be created", failure);
		}
	}

	private static RedisAgentStateStore.Builder applyKeyPrefix(
			RedisAgentStateStore.Builder builder, AgentStateStoreRedisConfig redis) {
		String keyPrefix = trimToNull(redis.getKeyPrefix());
		return keyPrefix != null ? builder.keyPrefix(keyPrefix) : builder;
	}

	private static String trimToNull(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.trim();
		return trimmed.isEmpty() ? null : trimmed;
	}
}
