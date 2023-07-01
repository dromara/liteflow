package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.util.ObjectUtil;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisParserHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RedisParserHelper.class);

    private RedisParserVO redisParserVO;

    private final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

    private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    private static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    private static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    private static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    private RedissonClient redissonClient;

    public RedisParserHelper(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try{
            try{
                this.redissonClient = ContextAwareHolder.loadContextAware().getBean(RedissonClient.class);
            }
            catch (Exception ignored){
            }
            if(ObjectUtil.isNull(redissonClient)){
                //todo get client
            }
        }
        catch (Exception e){
            throw new RedisException(e.getMessage());
        }

    }
}
