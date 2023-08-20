package com.yomahub.liteflow.test.redis;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.api.redisnode.RedisNodes;
import org.redisson.api.redisnode.RedisSingle;
import org.redisson.config.Config;

/**
 * 判断本地是否启动Redis
 *
 * @author hxinyu
 * @since 2.11.0
 */
public class RedisSubscribeTestCondition {

    /* 若6379端口未启动Redis则返回true */
    public static boolean notStartRedis() {
        try{
            Config config = new Config();
            config.useSingleServer().setAddress("redis://127.0.0.1:6379");
            RedissonClient redissonClient = Redisson.create(config);
            RedisSingle redisNode = redissonClient.getRedisNodes(RedisNodes.SINGLE);
            return !redisNode.pingAll();
        } catch (Exception e) {
            return true;
        }
    }
}
