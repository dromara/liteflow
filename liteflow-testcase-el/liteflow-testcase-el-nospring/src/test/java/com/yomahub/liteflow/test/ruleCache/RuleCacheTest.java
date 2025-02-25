package com.yomahub.liteflow.test.ruleCache;

import cn.hutool.core.collection.CollUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.lifecycle.PostProcessChainExecuteLifeCycle;
import com.yomahub.liteflow.lifecycle.impl.RuleCacheLifeCycle;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 非Spring环境下的规则缓存测试
 * @author DaleLee
 */
public class RuleCacheTest extends BaseTest {

    private static FlowExecutor flowExecutor;

    @BeforeAll
    public static void init() {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("ruleCache/flow.el.xml");
        config.setRuleCacheEnabled(true);
        config.setRuleCacheCapacity(5);
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    @BeforeEach
    public void reload() {
        flowExecutor.reloadRule();
        // 清空缓存
        Cache<String, RuleCacheLifeCycle.ChainState> cache = getCache();
        cache.invalidateAll();
        cache.cleanUp();
    }

    // 测试chain被淘汰
    @Test
    public void testRuleCache1() {
        // 加满缓存
        loadCache();
        // 缓存快照
        HashSet<@NonNull String> strings = CollUtil.newHashSet(getCache().asMap().keySet());
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("c==>b", response.getExecuteStepStr());
        // 获得被淘汰chain
        String chainId = getEvictedChain(strings);
        testEvicted(chainId);
        // 测试被淘汰的chain仍可正常执行
        response = flowExecutor.execute2Resp(chainId, "arg");
        Assertions.assertTrue(response.isSuccess());
    }

    // 测试缓存数量上限（串行）
    @Test
    public void testRuleCache2() {
        // 确保至少执行过5个不同的chain
        loadCache();
        // 随机执行chain
        Random random = new Random();
        for (int i = 0; i < 100; i++) {
            int id = random.nextInt(10) + 1;
            flowExecutor.execute2Resp("chain" + id);
        }
        // 等待缓存淘汰
        getCache().cleanUp();
        Assertions.assertEquals(10, FlowBus.getChainMap().size());
        // 测试只有5个chain被编译
        int count = 0;
        for (Chain chain : FlowBus.getChainMap().values()) {
            List<Condition> conditionList = chain.getConditionList();
            if (chain.isCompiled()) {
                Assertions.assertTrue(CollUtil.isNotEmpty(conditionList));
                count++;
            } else {
                Assertions.assertNull(conditionList);
            }
        }
        Assertions.assertTrue(count <= 5);
    }

    // 测试缓存数量上限（并行）
    @Test
    public void testRuleCache3() {
        loadCache();
        Random random = new Random();
        List<Future<LiteflowResponse>> futureList = CollUtil.newArrayList();
        for (int i = 0; i < 100; i++) {
            int id = random.nextInt(10) + 1;
            Future<LiteflowResponse> future = flowExecutor.execute2Future("chain" + id, "arg");
            futureList.add(future);
        }
        futureList.forEach(future -> {
            try {
                LiteflowResponse response = future.get();
                Assertions.assertTrue(response.isSuccess());
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        // 等待缓存淘汰
        getCache().cleanUp();
        Assertions.assertEquals(10, FlowBus.getChainMap().size());
        // 测试只有5个chain被编译
        int count = 0;
        for (Chain chain : FlowBus.getChainMap().values()) {
            List<Condition> conditionList = chain.getConditionList();
            if (chain.isCompiled()) {
                Assertions.assertTrue(CollUtil.isNotEmpty(conditionList));
                count++;
            } else {
                Assertions.assertNull(conditionList);
            }
        }
        Assertions.assertTrue(count <= 5);
    }

    // 测试开启规则缓存后，进入缓存的chain可以正常被更新
    @Test
    public void testRuleCache4() {
        loadCache();
        // 缓存快照
        HashSet<@NonNull String> strings = CollUtil.newHashSet(getCache().asMap().keySet());
        LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>b", response.getExecuteStepStr());
        // 获得被淘汰chain
        String chainId = getEvictedChain(strings);
        testEvicted(chainId);
        // chain7进入缓存
        Assertions.assertTrue(getCache().asMap().containsKey("chain7"));
        // 更新chain7
        LiteFlowChainELBuilder
                .createChain()
                .setChainId("chain7")
                .setEL("THEN(a, b, c)")
                .build();
        // 重新执行chain7
        response = flowExecutor.execute2Resp("chain7", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }

    // 测试开启规则缓存后，进入缓存的chain被移除后无法执行
    @Test
    public void testRuleCache5() {
        loadCache();
        // 缓存快照
        HashSet<@NonNull String> strings = CollUtil.newHashSet(getCache().asMap().keySet());
        LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>b", response.getExecuteStepStr());
        // 获得被淘汰chain
        String chainId = getEvictedChain(strings);
        testEvicted(chainId);
        // chain7进入缓存
        Assertions.assertTrue(getCache().asMap().containsKey("chain7"));
        // 手动移除chain7
        FlowBus.removeChain("chain7");
        response = flowExecutor.execute2Resp("chain7", "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(ChainNotFoundException.class, response.getCause().getClass());
    }

    // 测试并发下，正在执行的chain的condition被清理但仍能执行
    @Test
    public void testRuleCache6() throws InterruptedException {
        // 模拟清空编译好的chain
        Thread thread = new Thread(()-> {
            Chain chain1 = FlowBus.getChain("chain1");
            // 绕过初次编译
            chain1.setCompiled(true);
            chain1.setConditionList(null);
        });
        thread.start();
        thread.join();
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b", response.getExecuteStepStr());
    }

    // 加载缓存, chain1~chain5
    private void loadCache() {
        // 容量上限为5
        for (int i = 1; i <= 5; i++) {
            flowExecutor.execute2Resp("chain" + i);
        }
    }

    // 测试 chain 被淘汰
    private void testEvicted(String chanId) {
        Chain chain = FlowBus.getChain(chanId);
        getCache().cleanUp();
        // 测试缓存中不存在
        Assertions.assertFalse(getCache().asMap().containsKey(chanId));
        // 测试chain被设置为未编译
        Assertions.assertFalse(chain.isCompiled());
        Assertions.assertNull(chain.getConditionList());
    }

    public Cache<String, RuleCacheLifeCycle.ChainState> getCache() {
        List<PostProcessChainExecuteLifeCycle> lifeCycleList
                = LifeCycleHolder.getPostProcessChainExecuteLifeCycleList();
        for (PostProcessChainExecuteLifeCycle lifeCycle : lifeCycleList) {
            if (lifeCycle.getClass().equals(RuleCacheLifeCycle.class)) {
                RuleCacheLifeCycle ruleCacheLifeCycle = (RuleCacheLifeCycle) lifeCycle;
                return ruleCacheLifeCycle.getCache();
            }
        }
        return null;
    }

    // 获得淘汰的chain，传入淘汰前的chain集合
    // 确保只有一个被淘汰时使用
    String getEvictedChain(Set<String> set) {
        Cache<String, RuleCacheLifeCycle.ChainState> cache = getCache();
        cache.cleanUp();
        Set<@NonNull String> strings = cache.asMap().keySet();
        set.removeAll(strings);
        Assertions.assertEquals(1, set.size());
        return set.iterator().next();
    }
}
