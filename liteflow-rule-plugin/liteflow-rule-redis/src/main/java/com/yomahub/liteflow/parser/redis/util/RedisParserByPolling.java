package com.yomahub.liteflow.parser.redis.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import redis.clients.jedis.Jedis;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Redis 轮询机制实现类
 *
 * @author hxinyu
 * @since  2.10.6
 */

public class RedisParserByPolling implements RedisParserHelper{

    private final RedisParserVO redisParserVO;

    private Jedis chainJedis;

    private Jedis scriptJedis;

    //chainKey中chain总数
    private Integer chainFieldNum = 0;

    //scriptKey中script总数
    private Integer scriptFieldNum = 0;

    //chainKey中value的SHA1加密值 用于轮询时确定value是否变化
    private Map<String, String> chainSHAMap = new HashMap<>();

    //scriptKey中value的SHA1加密值 用于轮询时确定value是否变化
    private Map<String, String> scriptSHAMap = new HashMap<>();

    //计算hash中field数量的lua脚本
    private String luaOfKey = "local keys = redis.call(\"hkeys\", KEYS[1]);\n" +
            "return #keys;\n";

    //计算hash中value的SHA值的lua脚本
    private String luaOfValue = "local key = KEYS[1];\n" +
            "local field = KEYS[2];\n" +
            "local value, err = redis.call(\"hget\", key, field);\n" +
            "if value == false or value == nil then\n" +
            "    return \"nil\";\n" +
            "end\n" +
            "local sha1 = redis.sha1hex(value);\n" +
            "return sha1;";

    public RedisParserByPolling(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try{
            try{
                this.chainJedis = ContextAwareHolder.loadContextAware().getBean("chainJClient");
                this.scriptJedis = ContextAwareHolder.loadContextAware().getBean("scriptJClient");
            }
            catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainJedis)) {
                chainJedis = new Jedis(redisParserVO.getHost(), Integer.parseInt(redisParserVO.getPort()));
                if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
                    chainJedis.auth(redisParserVO.getPassword());
                }
                chainJedis.select(redisParserVO.getChainDataBase());
                //如果有脚本数据
                if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                    scriptJedis = new Jedis(redisParserVO.getHost(), Integer.parseInt(redisParserVO.getPort()));
                    if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
                        scriptJedis.auth(redisParserVO.getPassword());
                    }
                    scriptJedis.select(redisParserVO.getScriptDataBase());
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
            Set<String> chainNameSet = chainJedis.hkeys(chainKey);
            if (CollectionUtil.isEmpty(chainNameSet)) {
                throw new RedisException(StrUtil.format("There are no chains in key [{}]", chainKey));
            }
            chainFieldNum = chainNameSet.size();
            // 获取chainKey下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (String chainName : chainNameSet) {
                String chainData = chainJedis.hget(chainKey, chainName);
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(StrUtil.format(CHAIN_XML_PATTERN, chainName, chainData));
                }

                //计算该chainData的SHA值
                String chainSHA = DigestUtil.sha1Hex(chainData);
                chainSHAMap.put(chainName, chainSHA);
            }
            // 合并成所有chain的xml内容
            String chainAllContent = CollUtil.join(chainItemContentList, StrUtil.EMPTY);

            // 检查是否有脚本内容，如果有，进行脚本内容的获取
            String scriptAllContent = StrUtil.EMPTY;
            if (hasScript()) {
                String scriptKey = redisParserVO.getScriptKey();
                Set<String> scriptFieldSet = scriptJedis.hkeys(scriptKey);
                scriptFieldNum = scriptFieldSet.size();

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptFieldValue : scriptFieldSet) {
                    NodeSimpleVO nodeSimpleVO = convert(scriptFieldValue);
                    if (ObjectUtil.isNull(nodeSimpleVO)) {
                        throw new RedisException(
                                StrUtil.format("The name of the redis field [{}] in scriptKey [{}] is invalid",
                                        scriptFieldValue, scriptKey));
                    }
                    String scriptData = scriptJedis.hget(scriptKey, scriptFieldValue);

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

                    //计算scriptData的SHA值
                    String scriptSHA = DigestUtil.sha1Hex(scriptData);
                    scriptSHAMap.put(scriptFieldValue, scriptSHA);
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
        if (ObjectUtil.isNull(scriptJedis) || ObjectUtil.isNull(redisParserVO.getScriptDataBase())) {
            return false;
        }
        try{
            String scriptKey = redisParserVO.getScriptKey();
            if (StrUtil.isBlank(scriptKey)) {
                return false;
            }
            Set<String> scriptKeySet = scriptJedis.hkeys(scriptKey);
            return !CollUtil.isEmpty(scriptKeySet);
        }
        catch (Exception e) {
            return false;
        }
    }

    @Override
    public void listenRedis() {
        //将lua脚本添加到chainJedis脚本缓存
        String keyLuaOfChain = chainJedis.scriptLoad(luaOfKey);
        String valueLuaOfChain = chainJedis.scriptLoad(luaOfValue);

        //定时任务线程池
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(10);
        //轮询chain内容的定时任务
        pool.scheduleAtFixedRate(pollChainTask(keyLuaOfChain, valueLuaOfChain),
                60, Long.valueOf(redisParserVO.getPollingInterval()), TimeUnit.SECONDS);

        //如果有脚本内容
        if (ObjectUtil.isNotNull(scriptJedis) && ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
            String keyLuaOfScript = scriptJedis.scriptLoad(luaOfKey);
            String valueLuaOfScript = scriptJedis.scriptLoad(luaOfValue);
        }

    }


    /**
     * 用于轮询chain的定时任务
     */
    private Runnable pollChainTask(String keyLua, String valueLua) {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                String chainKey = redisParserVO.getChainKey();
                //先判断chainKey中chain数量有无增长
                String keyNum = chainJedis.evalsha(keyLua, 1, chainKey).toString();
                if (Integer.parseInt(keyNum) > chainFieldNum) {
                    //有新增加的chain,重新从redis中拉取chainId集合, 对比出新增的chain
                    Set<String> newChainSet = chainJedis.hkeys(chainKey);
                    Set<String> oldChainSet = chainSHAMap.keySet();
                    //求出差集,即新增的chain
                    Set<String> newAdd = new HashSet<>();
                    newAdd.addAll(newChainSet);
                    newAdd.removeAll(oldChainSet);
                    for (String newChainName : newAdd) {
                        String chainData = chainJedis.hget(chainKey, newChainName);
                        LiteFlowChainELBuilder.createChain().setChainId(newChainName).setEL(chainData).build();
                        LOG.info("starting poll flow config... update key={} new value={},", newChainName, chainData);

                        //修改SHAMap
                        chainSHAMap.put(newChainName, DigestUtil.sha1Hex(chainData));
                    }
                    //修改chainFieldNum
                    chainFieldNum = newChainSet.size();
                }

                //遍历Map,判断各个chain的值有无变化
                for(Map.Entry<String, String> entry: chainSHAMap.entrySet()) {
                    String chainName = entry.getKey();
                    String oldSHA = entry.getValue();
                    String newSHA = chainJedis.evalsha(valueLua, 2, chainKey, chainName).toString();
                    if (StrUtil.equals(newSHA, "nil")) {
                        //新SHA值为nil, 即未获取到该chain，表示该chain已被删除
                        FlowBus.removeChain(chainName);
                        LOG.info("starting reload flow config... delete key={}", chainName);

                        //修改SHAMap
                        chainSHAMap.remove(chainName);
                    }
                    else if (!StrUtil.equals(newSHA, oldSHA)) {
                        //SHA值发生变化 表示该chain的值已被修改 重新拉取变化的chain
                        String chainData = chainJedis.hget(chainKey, chainName);
                        LiteFlowChainELBuilder.createChain().setChainId(chainName).setEL(chainData).build();
                        LOG.info("starting poll flow config... update key={} new value={},", chainName, chainData);

                        //修改SHAMap
                        chainSHAMap.put(chainName, newSHA);
                    }
                    //SHA值无变化 表示该chain未改变
                }
            }
        };
        return r;
    }
}
