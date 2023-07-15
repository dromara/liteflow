package com.yomahub.liteflow.parser.redis.mode.polling;

import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 用于轮询chain的定时任务
 *
 * @author hxinyu
 * @since  2.10.6
 */
public class ChainPollingTask {

    private RedisParserVO redisParserVO;

    private Jedis chainJedis;

    private Integer chainNum;

    private Map<String, String> chainSHAMap;

    LFLog LOG;

    public ChainPollingTask(RedisParserVO redisParserVO, Jedis chainJedis, Integer chainNum, Map<String, String> chainSHAMap, LFLog LOG) {
        this.redisParserVO = redisParserVO;
        this.chainJedis = chainJedis;
        this.chainNum = chainNum;
        this.chainSHAMap = chainSHAMap;
        this.LOG = LOG;
    }


    /**
     * 用于返回chain轮询任务的Runnable实例
     * 先根据hash中value的SHA值修改变化的和被删除的chain
     * 再根据hash中field数量的变化拉取新增的chain
     */
    public Runnable pollChainTask(String keyLua, String valueLua) {
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
}
