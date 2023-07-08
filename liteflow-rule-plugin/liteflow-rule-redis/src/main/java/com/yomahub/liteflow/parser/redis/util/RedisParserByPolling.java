package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

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

    public RedisParserByPolling(RedisParserVO redisParserVO) {
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
                if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
                    chainClient.auth(redisParserVO.getPassword());
                }
                chainClient.select(redisParserVO.getChainDataBase());
                //如果有脚本数据
                if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                    scriptClient = new Jedis(redisParserVO.getHost(), Integer.parseInt(redisParserVO.getPort()));
                    if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
                        scriptClient.auth(redisParserVO.getPassword());
                    }
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
        try {
            // 检查chainKey下有没有子节点
            String chainKey = redisParserVO.getChainKey();
            Set<String> chainNameSet = chainClient.hkeys(chainKey);
            if (CollectionUtil.isEmpty(chainNameSet)) {
                throw new RedisException(StrUtil.format("There are no chains in key [{}]", chainKey));
            }
            // 获取chainKey下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (String chainName : chainNameSet) {
                String chainData = chainClient.hget(chainKey, chainName);
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainName, chainData));
                }
            }
            // 合并成所有chain的xml内容
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 检查是否有脚本内容，如果有，进行脚本内容的获取
            String scriptAllContent = StrUtil.EMPTY;
            if (hasScript()) {
                String scriptKey = redisParserVO.getScriptKey();
                Set<String> scriptFieldSet = scriptClient.hkeys(scriptKey);

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptFieldValue : scriptFieldSet) {
                    NodeSimpleVO nodeSimpleVO = convert(scriptFieldValue);
                    if (ObjectUtil.isNull(nodeSimpleVO)) {
                        throw new RedisException(
                                StrUtil.format("The name of the redis field [{}] in scriptKey [{}] is invalid",
                                        scriptFieldValue, scriptKey));
                    }
                    String scriptData = scriptClient.hget(scriptKey, scriptFieldValue);

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
        if (ObjectUtil.isNull(scriptClient) || ObjectUtil.isNull(redisParserVO.getScriptDataBase())) {
            return false;
        }
        try{
            String scriptKey = redisParserVO.getScriptKey();
            if (StrUtil.isBlank(scriptKey)) {
                return false;
            }
            Set<String> scriptKeySet = scriptClient.hkeys(scriptKey);
            return !CollUtil.isEmpty(scriptKeySet);
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public void listenRedis() {

    }
}
