package com.yomahub.liteflow.parser.redis;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.mode.RedisMode;
import com.yomahub.liteflow.parser.redis.mode.RedisParserHelper;
import com.yomahub.liteflow.parser.redis.mode.RedisParserMode;
import com.yomahub.liteflow.parser.redis.mode.polling.RedisParserPollingMode;
import com.yomahub.liteflow.parser.redis.mode.subscribe.RedisParserSubscribeMode;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

/**
 * Redis解析器实现，只支持EL形式的XML，不支持其他的形式
 *
 * @author hxinyu
 * @since  2.11.0
 */

public class RedisXmlELParser extends ClassXmlFlowELParser {

    private final RedisParserHelper redisParserHelper;

    private static final String ERROR_COMMON_MSG = "ruleSourceExtData or map is empty";

    private static final String ERROR_MSG_PATTERN = "ruleSourceExtData {} is blank";

    public RedisXmlELParser() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        try {
            RedisParserVO redisParserVO = null;
            String configJson;
            if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
                configJson = JsonUtil.toJsonString(liteflowConfig.getRuleSourceExtDataMap());
            }else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
                configJson = liteflowConfig.getRuleSourceExtData();
            }else{
                throw new RedisException(ERROR_COMMON_MSG);
            }

            redisParserVO = JsonUtil.parseObject(configJson, RedisParserVO.class);

            if (Objects.isNull(redisParserVO)) {
                throw new RedisException(ERROR_COMMON_MSG);
            }

            //检查配置文件
            checkParserVO(redisParserVO);

            //选择订阅机制 or 轮询机制
            RedisParserMode mode = redisParserVO.getMode();
            switch (mode) {
                case SUB:
                case SUBSCRIBE:
                    redisParserHelper = new RedisParserSubscribeMode(redisParserVO);
                    break;
                case POLL:
                default:
                    redisParserHelper = new RedisParserPollingMode(redisParserVO);
                    break;
            }

        }
        catch (RedisException redisException) {
            throw redisException;
        }
        catch (Exception e) {
            throw new RedisException(e);
        }
    }

    @Override
    public String parseCustom() {
        try {
            String content = redisParserHelper.getContent();
            FlowInitHook.addHook(() -> {
                redisParserHelper.listenRedis();
                return true;
            });
            return content;

        }
        catch (Exception e) {
            throw new RedisException(e);
        }
    }

    private void checkParserVO(RedisParserVO redisParserVO) {
        if (redisParserVO.getRedisMode().equals(RedisMode.SINGLE) && StrUtil.isBlank(redisParserVO.getHost())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "host"));
        }
        if (redisParserVO.getRedisMode().equals(RedisMode.SINGLE) && ObjectUtil.isNull(redisParserVO.getPort())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "port"));
        }
        if (redisParserVO.getRedisMode().equals(RedisMode.SENTINEL) && StrUtil.isBlank(redisParserVO.getMasterName())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "master name"));
        }
        if (redisParserVO.getRedisMode().equals(RedisMode.SENTINEL) && CollectionUtil.isEmpty(redisParserVO.getSentinelAddress())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "sentinel address list"));
        }
        if (ObjectUtil.isNull(redisParserVO.getChainDataBase())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "chainDataBase"));
        }
        if (StrUtil.isBlank(redisParserVO.getChainKey())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "chainKey"));
        }
    }
}
