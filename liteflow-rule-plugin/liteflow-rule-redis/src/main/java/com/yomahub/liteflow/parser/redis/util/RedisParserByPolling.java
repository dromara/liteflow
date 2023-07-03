package com.yomahub.liteflow.parser.redis.util;

import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;

/**
 * Redis 轮询机制实现类
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisParserByPolling implements RedisParserHelper{

    private final RedisParserVO redisParserVO;

    public RedisParserByPolling(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;
    }

    @Override
    public String getContent() {
        return null;
    }

    @Override
    public void listenRedis() {

    }
}
