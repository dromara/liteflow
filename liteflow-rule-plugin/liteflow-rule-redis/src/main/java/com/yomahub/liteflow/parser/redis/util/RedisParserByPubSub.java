package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryRemovedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Redis Pub/Sub机制实现类
 * Redisson客户端 RMapCache存储结构
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisParserByPubSub implements RedisParserHelper {

    private final RedisParserVO redisParserVO;

    private RedissonClient chainClient;

    private RedissonClient scriptClient;

    public RedisParserByPubSub(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try {
            try {
                this.chainClient = ContextAwareHolder.loadContextAware().getBean("chainClient");
                this.scriptClient = ContextAwareHolder.loadContextAware().getBean("scriptClient");
            }
            catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainClient)) {
                Config config = getRedissonConfig(redisParserVO,
                        Integer.parseInt(redisParserVO.getChainDataBase()));
                this.chainClient = Redisson.create(config);
                //如果有脚本数据
                if (StrUtil.isNotBlank(redisParserVO.getScriptDataBase())) {
                    config = getRedissonConfig(redisParserVO,
                            Integer.parseInt(redisParserVO.getScriptDataBase()));
                    this.scriptClient = Redisson.create(config);
                }
            }
        }
        catch (Exception e) {
            throw new RedisException(e.getMessage());
        }

    }

    private Config getRedissonConfig(RedisParserVO redisParserVO, Integer dataBase) {
        Config config = new Config();
        String redisAddress = StrFormatter.format(REDIS_URL_PATTERN, redisParserVO.getHost(), redisParserVO.getPort());
        if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
            config.useSingleServer().setAddress(redisAddress)
                    .setPassword(redisParserVO.getPassword())
                    .setDatabase(dataBase);
        }
        else {
            config.useSingleServer().setAddress(redisAddress)
                    .setDatabase(dataBase);
        }
        return config;
    }

    @Override
    public String getContent() {
        try {
            // 检查chainKey下有没有子节点
            RMapCache<String, String> chainKey = chainClient.getMapCache(redisParserVO.getChainKey());
            Set<String> chainNameSet = chainKey.keySet();
            if (CollectionUtil.isEmpty(chainNameSet)) {
                throw new RedisException(StrUtil.format("There are no chains in key [{}]",
                        redisParserVO.getChainKey()));
            }
            // 获取chainKey下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (String chainName : chainNameSet) {
                String chainData = chainKey.get(chainName);
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainName, chainData));
                }
            }
            // 合并成所有chain的xml内容
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 检查是否有脚本内容，如果有，进行脚本内容的获取
            String scriptAllContent = StrUtil.EMPTY;
            if (hasScript()) {
                RMapCache<String, String> scriptKey = scriptClient.getMapCache(redisParserVO.getScriptKey());
                Set<String> scriptKeySet = scriptKey.keySet();

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptKeyValue : scriptKeySet) {
                    NodeSimpleVO nodeSimpleVO = convert(scriptKeyValue);
                    if (Objects.isNull(nodeSimpleVO)) {
                        throw new RedisException(
                                StrUtil.format("The name of the redis key is invalid:{}", scriptKeyValue));
                    }
                    String scriptData = scriptKey.get(scriptKeyValue);

                    // 有语言类型
                    if (StrUtil.isNotBlank(nodeSimpleVO.getLanguage())) {
                        scriptItemContentList.add(StrUtil.format(NODE_ITEM_WITH_LANGUAGE_XML_PATTERN,
                                nodeSimpleVO.getNodeId(), nodeSimpleVO.getName(), nodeSimpleVO.getType(),
                                nodeSimpleVO.getLanguage(), scriptData));
                    }
                    // 没有语言类型
                    else {
                        scriptItemContentList.add(StrUtil.format(NODE_ITEM_XML_PATTERN, nodeSimpleVO.getNodeId(),
                                nodeSimpleVO.getName(), nodeSimpleVO.getType(), scriptData));
                    }

                    scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
                            CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
                }
            }

            return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
        }
        catch (Exception e) {
            throw new RedisException(e.getMessage());
        }
    }

    public boolean hasScript() {
        // 没有scriptClient或没有配置scriptDataBase
        if (Objects.isNull(scriptClient) || StrUtil.isBlank(redisParserVO.getScriptDataBase())) {
            return false;
        }
        try {
            // 存在这个节点，但是子节点不存在
            RMapCache<String, String> scriptKey = scriptClient.getMapCache(redisParserVO.getScriptKey());
            Set<String> scriptKeySet = scriptKey.keySet();
            return !CollUtil.isEmpty(scriptKeySet);
        }
        catch (Exception e) {
            return false;
        }
    }

    /**
     * 监听 redis key
     */
    @Override
    public void listenRedis() {
        //监听 chain
        RMapCache<String, String> chainKey = chainClient.getMapCache(redisParserVO.getChainKey());
        //添加新 chain
        chainKey.addListener((EntryCreatedListener<String, String>) event -> {
            LOG.info("starting reload flow config... create key={} value={},", event.getKey(), event.getValue());
            LiteFlowChainELBuilder.createChain().setChainId(event.getKey()).setEL(event.getValue()).build();
        });
        //修改 chain
        chainKey.addListener((EntryUpdatedListener<String, String>) event -> {
            LOG.info("starting reload flow config... update path={} new value={},", event.getKey(), event.getValue());
            LiteFlowChainELBuilder.createChain().setChainId(event.getKey()).setEL(event.getValue()).build();
        });
        //删除 chain
        chainKey.addListener((EntryRemovedListener<String, String>) event -> {
            LOG.info("starting reload flow config... delete key={}", event.getKey());
            FlowBus.removeChain(event.getKey());
        });

        //监听 script
        if (Objects.nonNull(scriptClient) && StrUtil.isNotBlank(redisParserVO.getScriptDataBase())) {
            RMapCache<String, String> scriptKey = scriptClient.getMapCache(redisParserVO.getScriptKey());
            //添加 script
            scriptKey.addListener((EntryCreatedListener<String, String>) event -> {
                LOG.info("starting reload flow config... create key={} value={},", event.getKey(), event.getValue());
                NodeSimpleVO nodeSimpleVO = convert(event.getKey());
                changeScriptNode(nodeSimpleVO, event.getValue());
            });
            //修改 script
            scriptKey.addListener((EntryUpdatedListener<String, String>) event -> {
                LOG.info("starting reload flow config... update path={} new value={},", event.getKey(), event.getValue());
                NodeSimpleVO nodeSimpleVO = convert(event.getKey());
                changeScriptNode(nodeSimpleVO, event.getValue());
            });
            //删除 script
            scriptKey.addListener((EntryRemovedListener<String, String>) event -> {
                LOG.info("starting reload flow config... delete key={}", event.getKey());
                NodeSimpleVO nodeSimpleVO = convert(event.getKey());
                FlowBus.getNodeMap().remove(nodeSimpleVO.getNodeId());
            });
        }
    }

    private void changeScriptNode(NodeSimpleVO nodeSimpleVO, String newValue) {
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
    }
}
