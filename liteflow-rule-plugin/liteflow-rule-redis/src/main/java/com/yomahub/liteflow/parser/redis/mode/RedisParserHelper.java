package com.yomahub.liteflow.parser.redis.mode;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import org.redisson.config.Config;
import org.redisson.config.SentinelServersConfig;
import org.redisson.config.SingleServerConfig;

/**
 * Redis 解析器通用接口
 *
 * @author hxinyu
 * @author Bryan.Zhang
 * @since 2.11.0
 */

public interface RedisParserHelper {

    LFLog LOG = LFLoggerManager.getLogger(RedisParserHelper.class);

    String SINGLE_REDIS_URL_PATTERN = "redis://{}:{}";

    String SENTINEL_REDIS_URL_PATTERN = "redis://{}";

    String CHAIN_XML_PATTERN = "<chain name=\"{}\">{}</chain>";

    String NODE_XML_PATTERN = "<nodes>{}</nodes>";

    String NODE_ITEM_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\"><![CDATA[{}]]></node>";

    String NODE_ITEM_WITH_LANGUAGE_XML_PATTERN = "<node id=\"{}\" name=\"{}\" type=\"{}\" language=\"{}\"><![CDATA[{}]]></node>";

    String XML_PATTERN = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><flow>{}{}</flow>";

    String getContent();

    void listenRedis();


    /**
     * 获取Redisson客户端的Config配置通用方法(单点模式)
     * @param redisParserVO redisParserVO
     * @param dataBase redis连接的数据库号
     * @return redisson config
     */
    default Config getSingleRedissonConfig(RedisParserVO redisParserVO, Integer dataBase) {
        Config config = new Config();
        String redisAddress = StrFormatter.format(SINGLE_REDIS_URL_PATTERN, redisParserVO.getHost(), redisParserVO.getPort());

        SingleServerConfig singleServerConfig = config.useSingleServer().setAddress(redisAddress)
                .setConnectionPoolSize(redisParserVO.getConnectionPoolSize())
                .setConnectionMinimumIdleSize(redisParserVO.getConnectionMinimumIdleSize())
                .setDatabase(dataBase);

        //如果配置了用户名和密码
        if (StrUtil.isNotBlank(redisParserVO.getUsername()) && StrUtil.isNotBlank(redisParserVO.getPassword())) {
            singleServerConfig.setUsername(redisParserVO.getUsername()).setPassword(redisParserVO.getPassword());
        }
        //如果配置了密码
        else if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
            singleServerConfig.setPassword(redisParserVO.getPassword());
        }
        return config;
    }

    /**
     * 获取Redisson客户端的Config配置通用方法(哨兵模式)
     * @param redisParserVO redisParserVO
     * @param dataBase redis连接的数据库号
     * @return redisson Config
     */
    default Config getSentinelRedissonConfig(RedisParserVO redisParserVO, Integer dataBase) {
        Config config = new Config();
        SentinelServersConfig sentinelConfig = config.useSentinelServers()
                .setMasterName(redisParserVO.getMasterName())
                .setMasterConnectionPoolSize(redisParserVO.getConnectionPoolSize())
                .setSlaveConnectionPoolSize(redisParserVO.getConnectionPoolSize())
                .setMasterConnectionMinimumIdleSize(redisParserVO.getConnectionMinimumIdleSize())
                .setSlaveConnectionMinimumIdleSize(redisParserVO.getConnectionMinimumIdleSize());
        redisParserVO.getSentinelAddress().forEach(address -> {
            sentinelConfig.addSentinelAddress(StrFormatter.format(SENTINEL_REDIS_URL_PATTERN, address));
        });
        //如果配置了用户名和密码
        if(StrUtil.isNotBlank(redisParserVO.getUsername()) && StrUtil.isNotBlank(redisParserVO.getPassword())) {
            sentinelConfig.setUsername(redisParserVO.getUsername())
                    .setPassword(redisParserVO.getPassword())
                    .setDatabase(dataBase);
        }
        //如果配置了密码
        else if(StrUtil.isNotBlank(redisParserVO.getPassword())) {
            sentinelConfig.setPassword(redisParserVO.getPassword())
                    .setDatabase(dataBase);
        }
        //没有配置密码
        else {
            sentinelConfig.setDatabase(dataBase);
        }
        return config;
    }

    /**
     * script节点的修改/添加
     *
     * @param scriptKeyValue 新的script名
     * @param newValue         新的script值
     */
    static boolean changeScriptNode(String scriptKeyValue, String newValue) {
        NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptKeyValue);

        if (BooleanUtil.isTrue(nodeSimpleVO.getEnable())){
            // 有语言类型
            if (StrUtil.isNotBlank(nodeSimpleVO.getLanguage())) {
                LiteFlowNodeBuilder.createScriptNode()
                        .setId(nodeSimpleVO.getNodeId())
                        .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                        .setName(nodeSimpleVO.getName())
                        .setScript(newValue)
                        .setLanguage(nodeSimpleVO.getLanguage())
                        .build();
            }
            // 没有语言类型
            else {
                LiteFlowNodeBuilder.createScriptNode()
                        .setId(nodeSimpleVO.getNodeId())
                        .setType(NodeTypeEnum.getEnumByCode(nodeSimpleVO.getType()))
                        .setName(nodeSimpleVO.getName())
                        .setScript(newValue)
                        .build();
            }
            return true;
        }else{
            FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
            return false;
        }
    }

    static void changeChain(String chainId, String value) {
        Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainId);
        // 如果是启用，就正常更新
        if (BooleanUtil.isTrue(pair.getKey())) {
            LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(value).build();
        }
        // 如果是禁用，就删除
        else {
            FlowBus.removeChain(chainId);
        }
    }
}
