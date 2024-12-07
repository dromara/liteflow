package com.yomahub.liteflow.test.ruleCache;

import cn.hutool.core.collection.CollUtil;
import com.github.benmanes.caffeine.cache.Cache;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.lifecycle.LifeCycleHolder;
import com.yomahub.liteflow.lifecycle.PostProcessFlowExecuteLifeCycle;
import com.yomahub.liteflow.lifecycle.impl.RuleCacheLifeCycle;
import com.yomahub.liteflow.test.BaseTest;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Springboot环境下规则缓存测试
 * @author DaleLee
 * @since 2.13.0
 */
@TestPropertySource(value = "classpath:/ruleCache/application.properties")
@SpringBootTest(classes = RuleCacheSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.ruleCache.cmp" })
public class RuleCacheSpringbootTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    @BeforeEach
    public void reload() {
        flowExecutor.reloadRule();
        // 清空缓存
        Cache<String, Object> cache = getCache();
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
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>b", response.getExecuteStepStr());
        // 获得被淘汰chain
        String chainId = getEvictedChain(strings);
        testEvicted(chainId);
        // 测试被淘汰的chain仍可正常执行
        response = flowExecutor.execute2Resp(chainId, "arg");
        Assertions.assertTrue(response.isSuccess());
    }

    // 测试缓存数量
    @Test
    public void testRuleCache2() {
        // 确保至少执行过3个不同的chain
        loadCache();
        // 随机执行chain
        loadCache(100);
        // 等待缓存淘汰
        getCache().cleanUp();
        // 测试只有3个chain被编译
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
        Assertions.assertEquals(3, count);
    }

    // 测试开启规则缓存后，进入缓存的chain可以正常被更新
    @Test
    public void testRuleCache3() {
        loadCache();
        // 缓存快照
        HashSet<@NonNull String> strings = CollUtil.newHashSet(getCache().asMap().keySet());
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>c", response.getExecuteStepStr());
        // chain5进入缓存
        Assertions.assertTrue(getCache().asMap().containsKey("chain5"));
        // 获得被淘汰chain
        String chainId = getEvictedChain(strings);
        testEvicted(chainId);
        // 更新chain5
        LiteFlowChainELBuilder
                .createChain()
                .setChainId("chain5")
                .setEL("THEN(a, b, c)")
                .build();
        // 重新执行chain5
        response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }

    // 测试开启规则缓存后，进入缓存的chain被移除后无法执行
    @Test
    public void testRuleCache4() {
        loadCache();
        // 缓存快照
        HashSet<@NonNull String> strings = CollUtil.newHashSet(getCache().asMap().keySet());
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>c", response.getExecuteStepStr());
        // chain5进入缓存
        Assertions.assertTrue(getCache().asMap().containsKey("chain5"));
        // 获得被淘汰chain
        String chainId = getEvictedChain(strings);
        testEvicted(chainId);
        // 手动移除chain5
        FlowBus.removeChain("chain5");
        response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(ChainNotFoundException.class, response.getCause().getClass());
    }

    // 测试并发下，正在执行的chain被淘汰仍能执行
    @Test
    public void testRuleCache5() throws InterruptedException {
        // 模拟清空编译好的chain
        Thread thread = new Thread(()-> {
            Chain chain1 = FlowBus.getChain("chain1");
            chain1.setCompiled(true);
            chain1.setConditionList(null);
        });
        thread.start();
        thread.join();
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b", response.getExecuteStepStr());
    }


    // 加载缓存, chain1、chain2、chain3
    private void loadCache() {
        // 容量上限为3
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b", response.getExecuteStepStr());
        response = flowExecutor.execute2Resp("chain2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>c", response.getExecuteStepStr());
        response = flowExecutor.execute2Resp("chain3", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("b==>c", response.getExecuteStepStr());
    }

    private void loadCache(int count) {
        // 随机执行chain
        Random random = new Random();
        for (int i = 0; i < count; i++) {
            int id = random.nextInt(5) + 1;
            flowExecutor.execute2Resp("chain" + id);
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

    public  Cache<String, Object> getCache() {
        List<PostProcessFlowExecuteLifeCycle> lifeCycleList
                = LifeCycleHolder.getPostProcessFlowExecuteLifeCycleList();
        for (PostProcessFlowExecuteLifeCycle lifeCycle : lifeCycleList) {
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
        Cache<String, Object> cache = getCache();
        cache.cleanUp();
        Set<@NonNull String> strings = cache.asMap().keySet();
        set.removeAll(strings);
        Assertions.assertEquals(1, set.size());
        return set.iterator().next();
    }
}
