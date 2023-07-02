package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.text.StrFormatter;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import org.redisson.Redisson;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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

    public String getContent(){
        try{
            // 检查chainKey下有没有子节点
            RMapCache<String, String> chainKey = chainClient.getMapCache(redisParserVO.getChainKey());
            Set<String> chainNameSet = chainKey.keySet();
            if (CollectionUtil.isEmpty(chainNameSet)) {
                throw new RedisException(StrUtil.format("There are no chains in key [{}]",
                        redisParserVO.getChainKey());
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
            if (hasScript()){
                RMapCache<String, String> scriptKey = scriptClient.getMapCache(redisParserVO.getScriptKey());
                Set<String> scriptKeySet = scriptKey.keySet();

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptKeyValue : scriptKeySet){
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
        catch (Exception e){
            throw new RedisException(e.getMessage());
        }
    }

    public boolean hasScript(){
        // 没有scriptClient或没有配置scriptDataBase
        if (Objects.isNull(scriptClient) || StrUtil.isNotBlank(redisParserVO.getScriptDataBase())) {
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

    public NodeSimpleVO convert(String str) {
        // 不需要去理解这串正则，就是一个匹配冒号的
        // 一定得是a:b，或是a:b:c...这种完整类型的字符串的
        List<String> matchItemList = ReUtil.findAllGroup0("(?<=[^:]:)[^:]+|[^:]+(?=:[^:])", str);
        if (CollUtil.isEmpty(matchItemList)) {
            return null;
        }

        NodeSimpleVO nodeSimpleVO = new NodeSimpleVO();
        if (matchItemList.size() > 1) {
            nodeSimpleVO.setNodeId(matchItemList.get(0));
            nodeSimpleVO.setType(matchItemList.get(1));
        }

        if (matchItemList.size() > 2) {
            nodeSimpleVO.setName(matchItemList.get(2));
        }

        if (matchItemList.size() > 3) {
            nodeSimpleVO.setLanguage(matchItemList.get(3));
        }

        return nodeSimpleVO;
    }

    private static class NodeSimpleVO {

        private String nodeId;

        private String type;

        private String name = StrUtil.EMPTY;

        private String language;

        public String getNodeId() {
            return nodeId;
        }

        public void setNodeId(String nodeId) {
            this.nodeId = nodeId;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getLanguage() {
            return language;
        }

        public void setLanguage(String language) {
            this.language = language;
        }

    }
}
