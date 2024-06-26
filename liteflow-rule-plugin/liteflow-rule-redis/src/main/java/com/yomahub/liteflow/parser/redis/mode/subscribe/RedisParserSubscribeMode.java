package com.yomahub.liteflow.parser.redis.mode.subscribe;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.mode.RedisMode;
import com.yomahub.liteflow.parser.redis.mode.RedisParserHelper;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import org.redisson.Redisson;
import org.redisson.api.map.event.EntryCreatedListener;
import org.redisson.api.map.event.EntryRemovedListener;
import org.redisson.api.map.event.EntryUpdatedListener;
import org.redisson.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Redis Pub/Sub机制实现类
 * 使用 Redisson客户端 RMapCache存储结构
 *
 * @author hxinyu
 * @since 2.11.0
 */

public class RedisParserSubscribeMode implements RedisParserHelper {

    private final RedisParserVO redisParserVO;

    private RClient chainClient;

    private RClient scriptClient;

    public RedisParserSubscribeMode(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try {
            try {
                this.chainClient = ContextAwareHolder.loadContextAware().getBean("chainClient");
                this.scriptClient = ContextAwareHolder.loadContextAware().getBean("scriptClient");
            } catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainClient)) {
                RedisMode redisMode = redisParserVO.getRedisMode();
                Config config;
                //Redis单点模式
                if (redisMode.equals(RedisMode.SINGLE)) {
                    config = getSingleRedissonConfig(redisParserVO, redisParserVO.getChainDataBase());
                    this.chainClient = new RClient(Redisson.create(config));
                    //如果有脚本数据
                    if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                        config = getSingleRedissonConfig(redisParserVO, redisParserVO.getScriptDataBase());
                        this.scriptClient = new RClient(Redisson.create(config));
                    }
                }

                //Redis哨兵模式
                else if (redisMode.equals(RedisMode.SENTINEL)) {
                    config = getSentinelRedissonConfig(redisParserVO, redisParserVO.getChainDataBase());
                    this.chainClient = new RClient(Redisson.create(config));
                    //如果有脚本数据
                    if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                        config = getSentinelRedissonConfig(redisParserVO, redisParserVO.getScriptDataBase());
                        this.scriptClient = new RClient(Redisson.create(config));
                    }
                }
            }
        } catch (Exception e) {
            throw new RedisException(e);
        }

    }

    @Override
    public String getContent() {
        try {
            // 检查chainKey下有没有子节点
            Map<String, String> chainMap = chainClient.getMap(redisParserVO.getChainKey());
            // 获取chainKey下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (Map.Entry<String, String> entry : chainMap.entrySet()) {
                String chainId = entry.getKey();
                String chainData = entry.getValue();
                RuleParsePluginUtil.ChainDto chainDto = RuleParsePluginUtil.parseChainKey(chainId);
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(chainDto.toElXml(chainData));
                }
            }
            // 合并成所有chain的xml内容
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 检查是否有脚本内容，如果有，进行脚本内容的获取
            String scriptAllContent = StrUtil.EMPTY;
            if (hasScript()) {
                Map<String, String> scriptMap = scriptClient.getMap(redisParserVO.getScriptKey());
                List<String> scriptItemContentList = new ArrayList<>();
                for (Map.Entry<String, String> entry : scriptMap.entrySet()) {
                    String scriptFieldValue = entry.getKey();
                    String scriptData = entry.getValue();
                    NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptFieldValue);
                    if (ObjectUtil.isNull(nodeSimpleVO)) {
                        throw new RedisException(
                                StrUtil.format("The name of the redis field [{}] in scriptKey [{}] is invalid",
                                        scriptFieldValue, redisParserVO.getScriptKey()));
                    }

                    nodeSimpleVO.setScript(scriptData);
                    scriptItemContentList.add(RuleParsePluginUtil.toScriptXml(nodeSimpleVO));
                }

                scriptAllContent = StrUtil.format(NODE_XML_PATTERN,
                        CollUtil.join(scriptItemContentList, StrUtil.EMPTY));
            }

            return StrUtil.format(XML_PATTERN, scriptAllContent, chainAllContent);
        } catch (Exception e) {
            throw new RedisException(e);
        }
    }

    public boolean hasScript() {
        // 没有scriptClient或没有配置scriptDataBase
        if (ObjectUtil.isNull(scriptClient) || ObjectUtil.isNull(redisParserVO.getScriptDataBase())) {
            return false;
        }
        try {
            // 存在这个节点，但是子节点不存在
            Map<String, String> scriptMap = scriptClient.getMap(redisParserVO.getScriptKey());
            return !CollUtil.isEmpty(scriptMap);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 监听 redis key
     */
    @Override
    public void listenRedis() {
        //监听 chain
        String chainKey = redisParserVO.getChainKey();

        //添加新 chain
        chainClient.addListener(chainKey, (EntryCreatedListener<String, String>) event -> {
            LOG.info("starting modify flow config... create key={} value={},", event.getKey(), event.getValue());
            String chainId = event.getKey();
            String value = event.getValue();
            RedisParserHelper.changeChain(chainId, value);
        });

        //修改 chain
        chainClient.addListener(chainKey, (EntryUpdatedListener<String, String>) event -> {
            LOG.info("starting modify flow config... create key={} value={},", event.getKey(), event.getValue());
            String chainId = event.getKey();
            String value = event.getValue();
            RedisParserHelper.changeChain(chainId, value);
        });

        //删除 chain
        chainClient.addListener(chainKey, (EntryRemovedListener<String, String>) event -> {
            LOG.info("starting reload flow config... delete key={}", event.getKey());
            Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(event.getKey());
            FlowBus.removeChain(pair.getValue());
        });

        //监听 script
        if (ObjectUtil.isNotNull(scriptClient) && ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
            String scriptKey = redisParserVO.getScriptKey();

            //添加 script
            scriptClient.addListener(scriptKey, (EntryCreatedListener<String, String>) event -> {
                LOG.info("starting reload flow config... create key={} value={},", event.getKey(), event.getValue());
                RedisParserHelper.changeScriptNode(event.getKey(), event.getValue());
            });
            //修改 script
            scriptClient.addListener(scriptKey, (EntryUpdatedListener<String, String>) event -> {
                LOG.info("starting reload flow config... update key={} new value={},", event.getKey(), event.getValue());
                RedisParserHelper.changeScriptNode(event.getKey(), event.getValue());
            });
            //删除 script
            scriptClient.addListener(scriptKey, (EntryRemovedListener<String, String>) event -> {
                LOG.info("starting reload flow config... delete key={}", event.getKey());
                NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(event.getKey());
                FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
            });
        }
    }
}
