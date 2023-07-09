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
    private Integer chainNum = 0;

    //scriptKey中script总数
    private Integer scriptNum = 0;

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
                this.chainJedis = ContextAwareHolder.loadContextAware().getBean("chainJedis");
                this.scriptJedis = ContextAwareHolder.loadContextAware().getBean("scriptJedis");
            }
            catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainJedis)) {
                chainJedis = new Jedis(redisParserVO.getHost(), redisParserVO.getPort());
                //如果配置了密码
                if (StrUtil.isNotBlank(redisParserVO.getPassword())) {
                    chainJedis.auth(redisParserVO.getPassword());
                }
                chainJedis.select(redisParserVO.getChainDataBase());
                //如果有脚本数据
                if (ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())) {
                    scriptJedis = new Jedis(redisParserVO.getHost(), redisParserVO.getPort());
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
            chainNum = chainNameSet.size();
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
                scriptNum = scriptFieldSet.size();

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

    /**
     * 定时轮询拉取Redis中变化的数据
     */
    @Override
    public void listenRedis() {
        //将lua脚本添加到chainJedis脚本缓存
        String keyLuaOfChain = chainJedis.scriptLoad(luaOfKey);
        String valueLuaOfChain = chainJedis.scriptLoad(luaOfValue);

        //定时任务线程池
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(10);
        //添加轮询chain的定时任务
        pool.scheduleAtFixedRate(pollChainTask(keyLuaOfChain, valueLuaOfChain),
                60, Long.valueOf(redisParserVO.getPollingInterval()), TimeUnit.SECONDS);

        //如果有脚本
        if (ObjectUtil.isNotNull(scriptJedis) && ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())
                && StrUtil.isNotBlank(redisParserVO.getScriptKey())) {
            //将lua脚本添加到scriptJedis脚本缓存
            String keyLuaOfScript = scriptJedis.scriptLoad(luaOfKey);
            String valueLuaOfScript = scriptJedis.scriptLoad(luaOfValue);
            //添加轮询script的定时任务
            pool.scheduleAtFixedRate(pollScriptTask(keyLuaOfScript, valueLuaOfScript),
                    60, Long.valueOf(redisParserVO.getPollingInterval()), TimeUnit.SECONDS);
        }
    }


    /**
     * 用于轮询chain的定时任务
     * 首先根据hash中field数量的变化拉取新增的chain
     * 再根据hash中value的SHA值修改变化的和被删除的chain
     */
    private Runnable pollChainTask(String keyLua, String valueLua) {
        Runnable r = () -> {
            String chainKey = redisParserVO.getChainKey();
            //Lua获取chainKey中最新的chain数量
            String keyNum = chainJedis.evalsha(keyLua, 1, chainKey).toString();
            //修改chainNum为最新chain数量
            chainNum = Integer.parseInt(keyNum);

            //遍历Map,判断各个chain的value有无变化：修改变化了值的chain和被删除的chain
            for(Map.Entry<String, String> entry: chainSHAMap.entrySet()) {
                String chainId = entry.getKey();
                String oldSHA = entry.getValue();
                //在redis服务端通过Lua脚本计算SHA值
                String newSHA = chainJedis.evalsha(valueLua, 2, chainKey, chainId).toString();
                if (StrUtil.equals(newSHA, "nil")) {
                    //新SHA值为nil, 即未获取到该chain,表示该chain已被删除
                    FlowBus.removeChain(chainId);
                    LOG.info("starting reload flow config... delete key={}", chainId);

                    //修改SHAMap
                    chainSHAMap.remove(chainId);
                }
                else if (!StrUtil.equals(newSHA, oldSHA)) {
                    //SHA值发生变化,表示该chain的值已被修改,重新拉取变化的chain
                    String chainData = chainJedis.hget(chainKey, chainId);
                    LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(chainData).build();
                    LOG.info("starting poll flow config... update key={} new value={},", chainId, chainData);

                    //修改SHAMap
                    chainSHAMap.put(chainId, newSHA);
                }
                //SHA值无变化,表示该chain未改变
            }

            //处理新添加chain和chainId被修改的情况
            if (chainNum > chainSHAMap.size()) {
                //如果封装的SHAMap数量比最新chain总数少, 说明有两种情况：
                // 1、添加了新chain
                // 2、修改了chainId:因为遍历到旧的id时会取到nil,SHAMap会把原来的chainId删掉,但没有机会添加新的chainId
                // 3、上述两者结合
                //在此处重新拉取所有chainId集合,补充添加新chain
                Set<String> newChainSet = chainJedis.hkeys(chainKey);
                for (String chainId : newChainSet) {
                    if (chainSHAMap.get(chainId) == null) {
                        //将新chainId添加到LiteFlowChainELBuilder和SHAMap
                        String chainData = chainJedis.hget(chainKey, chainId);
                        LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(chainData).build();
                        LOG.info("starting poll flow config... update key={} new value={},", chainId, chainData);
                        chainSHAMap.put(chainId, DigestUtil.sha1Hex(chainData));
                    }
                }
            }
        };
        return r;
    }

    /**
     * 用于轮询script的定时任务
     * 首先根据hash中field数量的变化拉取新增的script
     * 再根据hash中value的SHA值修改变化的和被删除的script
     */
    private Runnable pollScriptTask(String keyLua, String valueLua) {
        Runnable r = () -> {
            String scriptKey = redisParserVO.getScriptKey();
            //Lua获取scriptKey中最新的script数量
            String keyNum = scriptJedis.evalsha(keyLua, 1, scriptKey).toString();
            //修改scriptNum为最新script数量
            scriptNum = Integer.parseInt(keyNum);

            //遍历Map,判断各个script的value有无变化：修改变化了值的script和被删除的script
            for(Map.Entry<String, String> entry: scriptSHAMap.entrySet()) {
                String scriptFieldValue = entry.getKey();
                String oldSHA = entry.getValue();
                //在redis服务端通过Lua脚本计算SHA值
                String newSHA = scriptJedis.evalsha(valueLua, 2, scriptKey, scriptFieldValue).toString();
                if (StrUtil.equals(newSHA, "nil")) {
                    //新SHA值为nil, 即未获取到该script,表示该script已被删除
                    NodeSimpleVO nodeSimpleVO = convert(scriptFieldValue);
                    FlowBus.getNodeMap().remove(nodeSimpleVO.getNodeId());
                    LOG.info("starting reload flow config... delete key={}", scriptFieldValue);

                    //修改SHAMap
                    scriptSHAMap.remove(scriptFieldValue);
                }
                else if (!StrUtil.equals(newSHA, oldSHA)) {
                    //SHA值发生变化,表示该script的值已被修改,重新拉取变化的script
                    String scriptData = scriptJedis.hget(scriptKey, scriptFieldValue);
                    changeScriptNode(scriptFieldValue, scriptData);
                    LOG.info("starting reload flow config... update key={} new value={},", scriptFieldValue, scriptData);

                    //修改SHAMap
                    scriptSHAMap.put(scriptFieldValue, newSHA);
                }
                //SHA值无变化,表示该script未改变
            }

            //处理新添加script和script名被修改的情况
            if (scriptNum > scriptSHAMap.size()) {
                //如果封装的SHAMap数量比最新script总数少, 说明有两种情况：
                // 1、添加了新script
                // 2、修改了script名:因为遍历到旧的id时会取到nil,SHAMap会把原来的script删掉,但没有机会添加新的script
                // 3、上述两者结合
                //在此处重新拉取所有script名集合,补充添加新script
                Set<String> newScriptSet = scriptJedis.hkeys(scriptKey);
                for (String scriptFieldValue : newScriptSet) {
                    if (scriptSHAMap.get(scriptFieldValue) == null) {
                        //将新script添加到LiteFlowChainELBuilder和SHAMap
                        String scriptData = scriptJedis.hget(scriptKey, scriptFieldValue);
                        changeScriptNode(scriptFieldValue, scriptData);
                        LOG.info("starting reload flow config... update key={} new value={},", scriptFieldValue, scriptData);
                        scriptSHAMap.put(scriptFieldValue, DigestUtil.sha1Hex(scriptData));
                    }
                }
            }
        };
        return r;
    }
}
