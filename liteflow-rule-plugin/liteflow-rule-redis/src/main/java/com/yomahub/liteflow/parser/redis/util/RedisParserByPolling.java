package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import redis.clients.jedis.Jedis;

/**
 * Redis 轮询机制实现类
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisParserByPolling implements RedisParserHelper{

    private final RedisParserVO redisParserVO;

    private Jedis chainClient;

    private Jedis scriptClient;

    public
    RedisParserByPolling(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try{
            try{
                this.chainClient = ContextAwareHolder.loadContextAware().getBean("chainJClient");
                this.scriptClient = ContextAwareHolder.loadContextAware().getBean("scriptJClient");
            }
            catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainClient)) {
                chainClient = new Jedis(redisParserVO.getHost(), Integer.parseInt(redisParserVO.getPort()));
                chainClient.select(redisParserVO.getChainDataBase());
                //如果有脚本数据
                if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                    scriptClient = new Jedis(redisParserVO.getHost(), Integer.parseInt(redisParserVO.getPort()));
                    scriptClient.select(redisParserVO.getScriptDataBase());
                }
            }
        }
        catch (Exception e) {
            throw new RedisException(e.getMessage());
        }
    }

    @Override
    public String getContent() {
        return null;
    }

    @Override
    public void listenRedis() {

    }
}
