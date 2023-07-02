package com.yomahub.liteflow.parser.redis.util;

import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;

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
