package com.yomahub.liteflow.parser.redis.mode.subscribe;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.mode.RedisParserHelper;
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
import java.util.Set;

/**
 * Redis Pub/Sub机制实现类
 * Redisson客户端 RMapCache存储结构
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisParserSubscribeMode implements RedisParserHelper {

    private final RedisParserVO redisParserVO;

    private RedissonClient chainClient;

    private RedissonClient scriptClient;

    public RedisParserSubscribeMode(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try {
            try {
                this.chainClient = ContextAwareHolder.loadContextAware().getBean("chainClient");
                this.scriptClient = ContextAwareHolder.loadContextAware().getBean("scriptClient");
            }
            catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainClient)) {
                Config config = getRedissonConfig(redisParserVO, redisParserVO.getChainDataBase());
                this.chainClient = Redisson.create(config);
                //如果有脚本数据
                if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                    config = getRedissonConfig(redisParserVO, redisParserVO.getScriptDataBase());
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
        //如果配置了密码
        if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
            config.useSingleServer().setAddress(redisAddress)
                    .setPassword(redisParserVO.getPassword())
                    .setDatabase(dataBase);
        }
        //没有配置密码
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
            for (String chainId : chainNameSet) {
                String chainData = chainKey.get(chainId);
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainId, chainData));
                }
            }
            // 合并成所有chain的xml内容
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 检查是否有脚本内容，如果有，进行脚本内容的获取
            String scriptAllContent = StrUtil.EMPTY;
            if (hasScript()) {
                RMapCache<String, String> scriptKey = scriptClient.getMapCache(redisParserVO.getScriptKey());
                Set<String> scriptFieldSet = scriptKey.keySet();

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptFieldValue : scriptFieldSet) {
                    NodeSimpleVO nodeSimpleVO = RedisParserHelper.convert(scriptFieldValue);
                    if (ObjectUtil.isNull(nodeSimpleVO)) {
                        throw new RedisException(
                                StrUtil.format("The name of the redis field [{}] in scriptKey [{}] is invalid",
                                        scriptFieldValue, scriptKey));
                    }
                    String scriptData = scriptKey.get(scriptFieldValue);

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
                }

                scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
                        CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
            }

            return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
        }
        catch (Exception e) {
            throw new RedisException(e.getMessage());
        }
    }

    public boolean hasScript() {
        // 没有scriptClient或没有配置scriptDataBase
        if (ObjectUtil.isNull(scriptClient) || ObjectUtil.isNull(redisParserVO.getScriptDataBase())) {
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
            LOG.info("starting reload flow config... update key={} new value={},", event.getKey(), event.getValue());
            LiteFlowChainELBuilder.createChain().setChainId(event.getKey()).setEL(event.getValue()).build();
        });
        //删除 chain
        chainKey.addListener((EntryRemovedListener<String, String>) event -> {
            LOG.info("starting reload flow config... delete key={}", event.getKey());
            FlowBus.removeChain(event.getKey());
        });

        //监听 script
        if (ObjectUtil.isNotNull(scriptClient) && ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
            RMapCache<String, String> scriptKey = scriptClient.getMapCache(redisParserVO.getScriptKey());
            //添加 script
            scriptKey.addListener((EntryCreatedListener<String, String>) event -> {
                LOG.info("starting reload flow config... create key={} value={},", event.getKey(), event.getValue());
                RedisParserHelper.changeScriptNode(event.getKey(), event.getValue());
            });
            //修改 script
            scriptKey.addListener((EntryUpdatedListener<String, String>) event -> {
                LOG.info("starting reload flow config... update key={} new value={},", event.getKey(), event.getValue());
                RedisParserHelper.changeScriptNode(event.getKey(), event.getValue());
            });
            //删除 script
            scriptKey.addListener((EntryRemovedListener<String, String>) event -> {
                LOG.info("starting reload flow config... delete key={}", event.getKey());
                NodeSimpleVO nodeSimpleVO = RedisParserHelper.convert(event.getKey());
                FlowBus.getNodeMap().remove(nodeSimpleVO.getNodeId());
            });
        }
    }
}
