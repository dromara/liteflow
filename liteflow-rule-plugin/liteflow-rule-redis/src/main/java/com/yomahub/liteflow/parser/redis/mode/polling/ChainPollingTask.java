package com.yomahub.liteflow.parser.redis.mode.polling;

import cn.hutool.core.lang.Pair;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.util.RuleParsePluginUtil;

import java.util.*;

/**
 * 用于轮询chain的定时任务
 *
 * @author hxinyu
 * @since  2.11.0
 */
public class ChainPollingTask implements Runnable {

    private RedisParserVO redisParserVO;

    private RClient chainClient;

    private Integer chainNum;

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

            List<String> needDelete = new ArrayList<>();
            //遍历Map,判断各个chain的value有无变化：修改变化了值的chain和被删除的chain
            for (Map.Entry<String, String> entry : chainSHAMap.entrySet()) {
                String chainId = entry.getKey();
                String oldSHA = entry.getValue();
                Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainId);
                // 如果是停用，就直接进删除
                if (pair.getKey()){
                    FlowBus.removeChain(pair.getValue());
                    needDelete.add(chainId);
                    continue;
                }

                //在redis服务端通过Lua脚本计算SHA值
                String newSHA = chainClient.evalSha(valueLua, chainKey, chainId);
                if (StrUtil.equals(newSHA, "nil")) {
                    //新SHA值为nil, 即未获取到该chain,表示该chain已被删除
                    FlowBus.removeChain(pair.getValue());
                    LOG.info("starting reload flow config... delete key={}", chainId);

                    //添加到待删除的list 后续统一从SHAMap中移除
                    //不在这里直接移除是为了避免先删除导致chainSHAMap并没有完全遍历完 chain删除不全
                    needDelete.add(chainId);
                }
                else if (!StrUtil.equals(newSHA, oldSHA)) {
                    //SHA值发生变化,表示该chain的值已被修改,重新拉取变化的chain
                    String chainData = chainClient.hget(chainKey, chainId);
                    LiteFlowChainELBuilder.createChain().setChainId(pair.getValue()).setEL(chainData).build();
                    LOG.info("starting reload flow config... update key={} new value={},", chainId, chainData);

                    //修改SHAMap
                    chainSHAMap.put(chainId, newSHA);
                }
                //SHA值无变化,表示该chain未改变
            }

            //统一从SHAMap中移除要删除的chain
            for (String chainId : needDelete) {
                chainSHAMap.remove(chainId);
            }

            //处理新添加chain和chainId被修改的情况
            if (chainNum > chainSHAMap.size()) {
                //如果封装的SHAMap数量比最新chain总数少, 说明有两种情况：
                // 1、添加了新chain
                // 2、修改了chainId:因为遍历到旧的id时会取到nil,SHAMap会把原来的chainId删掉,但没有机会添加新的chainId
                // 3、上述两者结合
                //在此处重新拉取所有chainId集合,补充添加新chain
                Set<String> newChainSet = chainClient.hkeys(chainKey);
                for (String chainId : newChainSet) {
                    Pair<Boolean/*启停*/, String/*id*/> pair = RuleParsePluginUtil.parseIdKey(chainId);

                    if (!chainSHAMap.containsKey(chainId)) {
                        //将新chainId添加到LiteFlowChainELBuilder和SHAMap
                        String chainData = chainClient.hget(chainKey, chainId);
                        // 如果是启用，才装配
                        if (pair.getKey()){
                            LiteFlowChainELBuilder.createChain().setChainId(pair.getValue()).setEL(chainData).build();
                            LOG.info("starting reload flow config... create key={} new value={},", chainId, chainData);
                            chainSHAMap.put(chainId, DigestUtil.sha1Hex(chainData));
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("[Exception during chain polling] " + e.getMessage(), e);
        }
    }
}
