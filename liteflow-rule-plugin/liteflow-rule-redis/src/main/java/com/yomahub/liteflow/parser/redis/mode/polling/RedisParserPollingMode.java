package com.yomahub.liteflow.parser.redis.mode.polling;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.thread.NamedThreadFactory;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import com.yomahub.liteflow.parser.helper.NodeConvertHelper;
import com.yomahub.liteflow.parser.redis.exception.RedisException;
import com.yomahub.liteflow.parser.redis.mode.RClient;
import com.yomahub.liteflow.parser.redis.mode.RedisMode;
import com.yomahub.liteflow.parser.redis.mode.RedisParserHelper;
import com.yomahub.liteflow.parser.redis.vo.RedisParserVO;
import com.yomahub.liteflow.spi.holder.ContextAwareHolder;
import com.yomahub.liteflow.util.RuleParsePluginUtil;
import org.redisson.Redisson;
import org.redisson.config.Config;

import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Redis 轮询机制实现类
 *
 * @author hxinyu
 * @since  2.11.0
 */

public class RedisParserPollingMode implements RedisParserHelper {

    private final RedisParserVO redisParserVO;

    private RClient chainClient;

    private RClient scriptClient;

    //chainKey中chain总数
    private Integer chainNum = 0;

    //scriptKey中script总数
    private Integer scriptNum = 0;

    //chainKey中value的SHA1加密值 用于轮询时确定value是否变化
    private Map<String, String> chainSHAMap = new HashMap<>();

    //scriptKey中value的SHA1加密值 用于轮询时确定value是否变化
    private Map<String, String> scriptSHAMap = new HashMap<>();

    //定时任务线程池核心线程数
    private static final int CORE_POOL_SIZE = 2;

    //定时任务线程池
    private static ScheduledThreadPoolExecutor pollExecutor;

    //计算hash中field数量的lua脚本
    private final String luaOfKey = "local keys = redis.call(\"hkeys\", KEYS[1]);\n" +
            "return #keys;\n";

    //计算hash中value的SHA值的lua脚本
    private final String luaOfValue = "local key = KEYS[1];\n" +
            "local field = KEYS[2];\n" +
            "local value, err = redis.call(\"hget\", key, field);\n" +
            "if value == false or value == nil then\n" +
            "    return \"nil\";\n" +
            "end\n" +
            "local sha1 = redis.sha1hex(value);\n" +
            "return sha1;";

    public RedisParserPollingMode(RedisParserVO redisParserVO) {
        this.redisParserVO = redisParserVO;

        try{
            try{
                this.chainClient = ContextAwareHolder.loadContextAware().getBean("chainClient");
                this.scriptClient = ContextAwareHolder.loadContextAware().getBean("scriptClient");
            }
            catch (Exception ignored) {
            }
            if (ObjectUtil.isNull(chainClient)) {
                RedisMode redisMode = redisParserVO.getRedisMode();
                Config config;
                //Redis单点模式
                if (redisMode.equals(RedisMode.SINGLE)){
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
            //创建定时任务线程池
            if (ObjectUtil.isNull(pollExecutor)) {
                ThreadFactory namedThreadFactory = new NamedThreadFactory("Redis-Polling-", false);
                pollExecutor = new ScheduledThreadPoolExecutor(
                        CORE_POOL_SIZE,
                        namedThreadFactory,
                        new ThreadPoolExecutor.DiscardOldestPolicy());
            }
        }
        catch (Exception e) {
            throw new RedisException(e);
        }
    }

    @Override
    public String getContent() {
        try {
            // 检查chainKey下有没有子节点
            String chainKey = redisParserVO.getChainKey();
            Set<String> chainNameSet = chainClient.hkeys(chainKey);
            chainNum = chainNameSet.size();
            // 获取chainKey下的所有子节点内容List
            List<String> chainItemContentList = new ArrayList<>();
            for (String chainName : chainNameSet) {
                String chainData = chainClient.hget(chainKey, chainName);
                RuleParsePluginUtil.ChainDto chainDto = RuleParsePluginUtil.parseChainKey(chainName);
                if (StrUtil.isNotBlank(chainData)) {
                    chainItemContentList.add(chainDto.toElXml(chainData));
                }else{
                    continue;
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
                Set<String> scriptFieldSet = scriptClient.hkeys(scriptKey);
                scriptNum = scriptFieldSet.size();

                List<String> scriptItemContentList = new ArrayList<>();
                for (String scriptFieldValue : scriptFieldSet) {
                    NodeConvertHelper.NodeSimpleVO nodeSimpleVO = NodeConvertHelper.convert(scriptFieldValue);
                    if (ObjectUtil.isNull(nodeSimpleVO)) {
                        throw new RedisException(
                                StrUtil.format("The name of the redis field [{}] in scriptKey [{}] is invalid",
                                        scriptFieldValue, scriptKey));
                    }

                    String scriptData = scriptClient.hget(scriptKey, scriptFieldValue);
                    nodeSimpleVO.setScript(scriptData);
                    scriptItemContentList.add(RuleParsePluginUtil.toScriptXml(nodeSimpleVO));

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
            throw new RedisException(e);
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

    /**
     * 定时轮询拉取Redis中变化的数据
     */
    @Override
    public void listenRedis() {
        //将lua脚本添加到chainJedis脚本缓存
        String keyLuaOfChain = chainClient.scriptLoad(luaOfKey);
        String valueLuaOfChain = chainClient.scriptLoad(luaOfValue);

        //添加轮询chain的定时任务
        ChainPollingTask chainTask = new ChainPollingTask(redisParserVO, chainClient, chainNum, chainSHAMap, keyLuaOfChain, valueLuaOfChain);
        pollExecutor.scheduleAtFixedRate(chainTask, redisParserVO.getPollingStartTime().longValue(),
                redisParserVO.getPollingInterval().longValue(), TimeUnit.SECONDS);

        //如果有脚本
        if (ObjectUtil.isNotNull(scriptClient) && ObjectUtil.isNotNull(redisParserVO.getScriptDataBase())
                && StrUtil.isNotBlank(redisParserVO.getScriptKey())) {
            //将lua脚本添加到scriptJedis脚本缓存
            String keyLuaOfScript = scriptClient.scriptLoad(luaOfKey);
            String valueLuaOfScript = scriptClient.scriptLoad(luaOfValue);

            //添加轮询script的定时任务
            ScriptPollingTask scriptTask = new ScriptPollingTask(redisParserVO, scriptClient, scriptNum, scriptSHAMap, keyLuaOfScript, valueLuaOfScript);
            pollExecutor.scheduleAtFixedRate(scriptTask, redisParserVO.getPollingStartTime().longValue(),
                    redisParserVO.getPollingInterval().longValue(), TimeUnit.SECONDS);
        }
    }
}
