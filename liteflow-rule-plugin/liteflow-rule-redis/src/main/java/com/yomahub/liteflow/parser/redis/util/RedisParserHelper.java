package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedisParserHelper {

    private static final Logger LOG = LoggerFactory.getLogger(RedisParserHelper.class);

    private RedisParserVO redisParserVO;

    private final String REDIS_URL_PATTERN = "redis://{}:{}";

    private final String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

    private final String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    private static final String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    private static final String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    private static final String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    private RedissonClient chainClient;

    private RedissonClient scriptClient;

    public RedisParserHelper(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try{
            try{
                this.chainClient = ContextAwareHolder.loadContextAware().getBean("chainClient");
                this.scriptClient = ContextAwareHolder.loadContextAware().getBean("scriptClient");
            }
            catch (Exception ignored){
            }
            if(ObjectUtil.isNull(chainClient)){
                Config config = new Config();
                config = getRedissonConfig(redisParserVO, config,
                        Integer.parseInt(redisParserVO.getChainDataBase()));
                this.chainClient = Redisson.create(config);
                //如果有脚本数据
                if (StrUtil.isNotBlank(redisParserVO.getScriptDataBase())){
                    config = getRedissonConfig(redisParserVO, config,
                            Integer.parseInt(redisParserVO.getScriptDataBase()));
                    this.scriptClient = Redisson.create(config);
                }
            }
        }
        catch (Exception e){
            throw new RedisException(e.getMessage());
        }

    }

    private Config getRedissonConfig(RedisParserVO redisParserVO, Config config, Integer dataBase){
        String redisAddress = StrFormatter.format(REDIS_URL_PATTERN, redisParserVO.getHost(), redisParserVO.getPort());
        if (StrUtil.isNotBlank(redisParserVO.getPassword())){
            config.useSingleServer().setAddress(redisAddress)
                    .setPassword(redisParserVO.getPassword())
                    .setDatabase(dataBase);
        } else{
            config.useSingleServer().setAddress(redisAddress)
                    .setDatabase(dataBase);
        }
        return config;
    }
}
