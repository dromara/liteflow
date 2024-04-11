package com.yomahub.liteflow.parser.redis.mode.polling;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.log.LFLog;
import com.yomahub.liteflow.log.LFLoggerManager;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.mode.RedisParserHelper;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 用于轮询script的定时任务
 *
 * @author hxinyu
 * @since 2.11.0
 */
public class ScriptPollingTask implements Runnable {

    private RedisParserVO redisParserVO;

    private RClient scriptClient;

    private Integer scriptNum;

    private Map<String, String> scriptSHAMap;

    private String keyLua;

    private String valueLua;

    LFLog LOG = LFLoggerManager.getLogger(ScriptPollingTask.class);

    public ScriptPollingTask(RedisParserVO redisParserVO, RClient scriptClient, Integer scriptNum, Map<String, String> scriptSHAMap, String keyLua, String valueLua) {
        this.redisParserVO = redisParserVO;
        this.scriptClient = scriptClient;
        this.scriptNum = scriptNum;
        this.scriptSHAMap = scriptSHAMap;
        this.keyLua = keyLua;
        this.valueLua = valueLua;
    }

    /**
     * 用于返回script轮询任务
     * 首先根据hash中field数量的变化拉取新增的script
     * 再根据hash中value的SHA值修改变化的和被删除的script
     */
    @Override
    public void run() {
        try {
            String scriptKey = redisParserVO.getScriptKey();
            //Lua获取scriptKey中最新的script数量
            String keyNum = scriptClient.evalSha(keyLua, scriptKey);
            //修改scriptNum为最新script数量
            scriptNum = Integer.parseInt(keyNum);

            List<String> needDelete = new ArrayList<>();
            //遍历Map,判断各个script的value有无变化：修改变化了值的script和被删除的script
            for (Map.Entry<String, String> entry : scriptSHAMap.entrySet()) {
                String scriptFieldValue = entry.getKey();
                String oldSHA = entry.getValue();
                //在redis服务端通过Lua脚本计算SHA值
                String newSHA = scriptClient.evalSha(valueLua, scriptKey, scriptFieldValue);
                if (StrUtil.equals(newSHA, "nil")) {
                    //新SHA值为nil, 即未获取到该script,表示该script已被删除
                    NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptFieldValue);
                    FlowBus.unloadScriptNode(nodeSimpleVO.getNodeId());
                    LOG.info("starting reload flow config... delete key={}", scriptFieldValue);

                    //添加到待删除的list 后续统一从SHAMap中移除
                    //不在这里直接移除是为了避免先删除导致scriptSHAMap并没有完全遍历完 script删除不全
                    needDelete.add(scriptFieldValue);
                } else if (!StrUtil.equals(newSHA, oldSHA)) {
                    //SHA值发生变化,表示该script的值已被修改,重新拉取变化的script
                    String scriptData = scriptClient.hget(scriptKey, scriptFieldValue);

                    boolean changeSuccess = RedisParserHelper.changeScriptNode(scriptFieldValue, scriptData);

                    if (BooleanUtil.isTrue(changeSuccess)){
                        scriptSHAMap.put(scriptFieldValue, newSHA);
                    }else{
                        needDelete.add(scriptFieldValue);
                    }
                }
                //SHA值无变化,表示该script未改变
            }

            //统一从SHAMap中移除要删除的script
            for (String scriptFieldValue : needDelete) {
                scriptSHAMap.remove(scriptFieldValue);
            }

            //处理新添加script和script名被修改的情况
            if (scriptNum > scriptSHAMap.size()) {
                //如果封装的SHAMap数量比最新script总数少, 说明有两种情况：
                // 1、添加了新script
                // 2、修改了script名:因为遍历到旧的id时会取到nil,SHAMap会把原来的script删掉,但没有机会添加新的script
                // 3、上述两者结合
                //在此处重新拉取所有script名集合,补充添加新script
                Set<String> newScriptSet = scriptClient.hkeys(scriptKey);
                for (String scriptFieldValue : newScriptSet) {
                    if (!scriptSHAMap.containsKey(scriptFieldValue)) {
                        //将新script添加到LiteFlowChainELBuilder和SHAMap
                        String scriptData = scriptClient.hget(scriptKey, scriptFieldValue);

                        boolean isAddSuccess = RedisParserHelper.changeScriptNode(scriptFieldValue, scriptData);

                        if (BooleanUtil.isTrue(isAddSuccess)){
                            LOG.info("starting reload flow config... create key={} new value={},", scriptFieldValue, scriptData);
                            scriptSHAMap.put(scriptFieldValue, DigestUtil.sha1Hex(scriptData));
                        }else{
                            LOG.info("starting reload flow config... delete key={}", scriptFieldValue);
                            needDelete.add(scriptFieldValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LOG.error("[Exception during script polling] " + e.getMessage(), e);
        }
    }
}