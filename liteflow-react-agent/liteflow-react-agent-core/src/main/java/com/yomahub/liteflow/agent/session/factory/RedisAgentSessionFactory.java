package com.yomahub.liteflow.agent.session.factory;

import com.yomahub.liteflow.agent.exception.AgentConfigException;
import com.yomahub.liteflow.property.agent.AgentConfig;
import com.yomahub.liteflow.property.agent.MemoryStorageMode;
import com.yomahub.liteflow.property.agent.RedisMemoryConfig;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import io.agentscope.core.session.Session;

/**
 * 通过把用户提供的 Redis 客户端 bean（Redisson、Jedis、Lettuce）
 * 适配到 AgentScope 的 RedisSession 来支持 {@link MemoryStorageMode#REDIS}。
 *
 * <p>Redis 客户端类通过反射查找，因此 core 模块不会对 Redisson、Jedis、Lettuce
 * 产生硬性的编译期依赖。如果选择 REDIS 模式但 classpath 中缺少匹配驱动，
 * 会在首次 {@code process()} 时失败，而不是在框架启动时失败。
 */
public class RedisAgentSessionFactory implements AgentSessionFactory {

    private static final String REDIS_SESSION_CLASS = "io.agentscope.core.session.redis.RedisSession";

    @Override
    public MemoryStorageMode mode() {
        return MemoryStorageMode.REDIS;
    }

    @Override
    public Session create(AgentConfig cfg) {
        RedisMemoryConfig rc = cfg.getSession().getMemory().getRedis();
        if (rc.getBeanName() == null || rc.getBeanName().trim().isEmpty()) {
            throw new AgentConfigException(
                    "liteflow.agent.session.memory.redis.beanName is required when mode=REDIS");
        }
        Object client = ContextAwareHolder.loadContextAware().getBean(rc.getBeanName());
        if (client == null) {
            throw new AgentConfigException("Redis client bean not found: " + rc.getBeanName());
        }
        String builderMethod;
        String clientFqn;
        switch (rc.getClientType()) {
            case REDISSON:
                builderMethod = "redissonClient";
                clientFqn = "org.redisson.api.RedissonClient";
                break;
            case JEDIS:
                builderMethod = "jedisClient";
                clientFqn = "redis.clients.jedis.UnifiedJedis";
                break;
            case LETTUCE:
                builderMethod = "lettuceClient";
                clientFqn = "io.lettuce.core.RedisClient";
                break;
            default:
                throw new AgentConfigException("Unsupported redis client type: " + rc.getClientType());
        }
        try {
            Class<?> sessionClass = Class.forName(REDIS_SESSION_CLASS);
            Object builder = sessionClass.getMethod("builder").invoke(null);
            Class<?> clientType = Class.forName(clientFqn);
            if (!clientType.isInstance(client)) {
                throw new AgentConfigException("Bean '" + rc.getBeanName() + "' is not a "
                        + clientFqn + "; got " + client.getClass().getName());
            }
            builder.getClass().getMethod(builderMethod, clientType).invoke(builder, client);
            if (rc.getKeyPrefix() != null && !rc.getKeyPrefix().isEmpty()) {
                builder.getClass().getMethod("keyPrefix", String.class).invoke(builder, rc.getKeyPrefix());
            }
            return (Session) builder.getClass().getMethod("build").invoke(builder);
        } catch (ClassNotFoundException e) {
            throw new AgentConfigException(
                    "Class not found while building RedisSession: " + e.getMessage()
                            + ". Add the matching driver dependency (Redisson/Jedis/Lettuce).", e);
        } catch (AgentConfigException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentConfigException("Failed to build RedisSession", e);
        }
    }
}
