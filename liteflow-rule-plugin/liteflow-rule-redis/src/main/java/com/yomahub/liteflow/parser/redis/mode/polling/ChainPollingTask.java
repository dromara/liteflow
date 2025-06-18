package com.yomahub.liteflow.parser.redis.mode.polling;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import com.yomahub.liteflow.util.TupleOf3;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 用于轮询chain的定时任务
 *
 * @author hxinyu
 * @author Bryan.Zhang
 * @since  2.11.0
 */
public class ChainPollingTask implements Runnable {

    private RedisParserVO redisParserVO;

    private RClient chainClient;

    private Integer chainNum;

    //key为chainId，value为缓存的SHA值
    private Map<String, String> chainSHAMap;

    private String keyLua;

    private String valueLua;

    LFLog LOG = LFLoggerManager.getLogger(ChainPollingTask.class);

    public ChainPollingTask(RedisParserVO redisParserVO, RClient chainClient, Integer chainNum, Map<String, String> chainSHAMap, String keyLua, String valueLua) {
        this.redisParserVO = redisParserVO;
        this.chainClient = chainClient;
        this.chainNum = chainNum;
        this.chainSHAMap = chainSHAMap;
        this.keyLua = keyLua;
        this.valueLua = valueLua;
    }

    /**
     * 用于返回chain轮询任务
     * 先根据hash中value的SHA值修改变化的和被删除的chain
     * 再根据hash中field数量的变化拉取新增的chain
     */
    @Override
    public void run() {
        try {
            String chainKey = redisParserVO.getChainKey();
            //Lua获取chainKey中最新的chain数量
            String keyNum = chainClient.evalSha(keyLua, chainKey);
            //修改chainNum为最新chain数量
            chainNum = Integer.parseInt(keyNum);

            //拿到所有的Chain的HashKey
            Set<String> newChainHashKeySet = chainClient.hkeys(chainKey);

            List<TupleOf3<String, String, Boolean>> tupleOf3List = newChainHashKeySet.stream().map(chainHashKey -> {
                Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainHashKey);
                //TupleOf3为三元值对象，在这里A为chainHashKey，B为解析到的chainId，C为是否启用
                return new TupleOf3<>(chainHashKey, pair.getValue(), pair.getKey());
            }).collect(Collectors.toList());

            tupleOf3List.forEach(tupleOf3 -> {
                String chainHashKey = tupleOf3.getA();
                String chainId = tupleOf3.getB();
                Boolean enable = tupleOf3.getC();

                // 如果是停用，就直接删除
                if (BooleanUtil.isFalse(enable)){
                    LOG.info("starting reload flow config... delete key={}", chainId);
                    chainSHAMap.remove(chainId);
                    FlowBus.removeChain(chainId);
                    return;
                }

                //在redis服务端通过Lua脚本计算SHA值
                String newSHA = chainClient.evalSha(valueLua, chainKey, chainHashKey);

                if (StrUtil.isBlank(newSHA) || "nil".equals(newSHA)){
                    FlowBus.removeChain(chainId);
                    return;
                }

                // chainSHAMap不含有redis取到的chainId，说明是新增的
                if (!chainSHAMap.containsKey(chainId)){
                    String chainEL = chainClient.hget(chainKey, chainHashKey);
                    LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(chainEL).build();
                    chainSHAMap.put(chainId, newSHA);
                }else{
                    String oldSHA = chainSHAMap.get(chainId);
                    if (!StrUtil.equals(newSHA, oldSHA)) {
                        //SHA值发生变化,表示该chain的值已被修改,重新拉取变化的chain
                        String chainEL = chainClient.hget(chainKey, chainHashKey);
                        LiteFlowChainELBuilder.createChain().setChainId(chainId).setEL(chainEL).build();
                        LOG.info("starting reload flow config... update key={} new value={},", chainId, chainEL);
                        //修改SHAMap
                        chainSHAMap.put(chainId, newSHA);
                    }
                }
            });

            // 这里是为了处理在redis服务端删除，但是本地缓存还存在chainId的情况
            // 这表明是服务端这边已经删除了chain
            if (CollectionUtil.isNotEmpty(chainSHAMap)){
                Set<String> newChainIdSet = tupleOf3List.stream().map(TupleOf3::getB).collect(Collectors.toSet());

                Collection<String> deletedChainIdSet = CollectionUtil.subtract(chainSHAMap.keySet(), newChainIdSet);

                deletedChainIdSet.forEach(chainId -> {
                    chainSHAMap.remove(chainId);
                    FlowBus.removeChain(chainId);
                    LOG.info("starting reload flow config... delete key={}", chainId);
                });
            }
        } catch (Exception e) {
            LOG.error("[Exception during chain polling] " + e.getMessage(), e);
        }
    }
}
