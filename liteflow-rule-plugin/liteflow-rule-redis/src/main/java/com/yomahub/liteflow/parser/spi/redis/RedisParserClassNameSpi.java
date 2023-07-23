package com.yomahub.liteflow.parser.spi.redis;

import com.yomahub.liteflow.parser.redis.RedisXmlELParser;
import com.yomahub.liteflow.parser.spi.ParserClassNameSpi;

/**
 * Redis 解析器 SPI 实现
 *
 *  @author hxinyu
 *  @since  2.11.0
 */
public class RedisParserClassNameSpi implements ParserClassNameSpi {

    @Override
    public String getSpiClassName() {
        return RedisXmlELParser.class.getName();
    }
}
