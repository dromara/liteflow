package com.yomahub.liteflow.test.redis;

import cn.hutool.core.util.StrUtil;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.api.redisnode.RedisSingle;
import org.redisson.config.Config;

import java.util.concurrent.TimeUnit;

/**
 * 判断本地6379端口是否启动了Redis
 */
public class RedisSubscribeTestCondition {

    /**
     * @return true为本地未启动Redis
     */
    public static boolean notStartRedis() {
        try{
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            RedissonClient redissonClient = Redisson.create(config);
            RedisSingle redisNode = redissonClient.getRedisNodes(RedisNodes.SINGLE);
            return !redisNode.pingAll(15000, TimeUnit.MICROSECONDS);
        } catch (Exception e) {
            return true;
        }
    }
}
