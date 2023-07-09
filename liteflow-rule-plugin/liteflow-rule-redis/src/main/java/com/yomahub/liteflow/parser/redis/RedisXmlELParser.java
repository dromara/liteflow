package com.yomahub.liteflow.parser.redis;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.map.MapUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.core.FlowInitHook;
import com.yomahub.liteflow.parser.el.ClassXmlFlowELParser;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.util.RedisParserByPolling;
import com.yomahub.liteflow.parser.redis.util.RedisParserBySubscribe;
import com.yomahub.liteflow.parser.redis.util.RedisParserHelper;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.property.LiteflowConfigGetter;
import com.yomahub.liteflow.util.JsonUtil;

import java.util.Objects;

/**
 * Redis解析器实现，只支持EL形式的XML，不支持其他的形式
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisXmlELParser extends ClassXmlFlowELParser {

    private final RedisParserHelper redisParserHelper;

    private static final String ERROR_COMMON_MSG = "ruleSourceExtData or map is empty";

    private static final String ERROR_MSG_PATTERN = "ruleSourceExtData {} is blank";

    public RedisXmlELParser() {
        LiteflowConfig liteflowConfig = LiteflowConfigGetter.get();

        try {
            RedisParserVO redisParserVO = null;
            if (MapUtil.isNotEmpty((liteflowConfig.getRuleSourceExtDataMap()))) {
                redisParserVO = BeanUtil.toBean(liteflowConfig.getRuleSourceExtDataMap(),
                        RedisParserVO.class, CopyOptions.create());
            }
            else if (StrUtil.isNotBlank(liteflowConfig.getRuleSourceExtData())) {
                redisParserVO = JsonUtil.parseObject(liteflowConfig.getRuleSourceExtData(), RedisParserVO.class);
            }
            if (Objects.isNull(redisParserVO)) {
                throw new RedisException(ERROR_COMMON_MSG);
            }

            //检查配置文件
            checkParserVO(redisParserVO);

            //选择订阅机制 or 轮询机制
            if (StrUtil.equalsIgnoreCase("subscribe", redisParserVO.getMode())) {
                redisParserHelper = new RedisParserBySubscribe(redisParserVO);
            } else {
                redisParserHelper = new RedisParserByPolling(redisParserVO);
            }

        }
        catch (RedisException redisException) {
            throw redisException;
        }
        catch (Exception e) {
            throw new RedisException(e.getMessage());
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
            throw new RedisException(e.getMessage());
        }
    }

    private void checkParserVO(RedisParserVO redisParserVO) {
        if (StrUtil.isBlank(redisParserVO.getHost())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "host"));
        }
        if (ObjectUtil.isNull(redisParserVO.getPort())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "port"));
        }
        if (ObjectUtil.isNull(redisParserVO.getChainDataBase())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "chainDataBase"));
        }
        if (StrUtil.isBlank(redisParserVO.getChainKey())) {
            throw new RedisException(StrFormatter.format(ERROR_MSG_PATTERN, "chainKey"));
        }
    }
}
