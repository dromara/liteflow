package com.yomahub.liteflow.test.ruleCache;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.exception.ChainNotFoundException;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

/**
 * Springboot环境下规则缓存测试
 * @author DaleLee
 */
@TestPropertySource(value = "classpath:/ruleCache/application.properties")
@SpringBootTest(classes = RuleCacheSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.ruleCache.cmp" })
public class RuleCacheSpringbootTest extends BaseTest {
    @Resource
    private FlowExecutor flowExecutor;

    // 测试chain被淘汰
    @Test
    public void testRuleCache1() throws InterruptedException {
        flowExecutor.reloadRule();
        // 加满缓存
        loadCache();
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        // chain1 被淘汰
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>b", response.getExecuteStepStr());
        testEvicted("chain1");
    }

    // 测试缓存数量
    @Test
    public void testRuleCache2() throws InterruptedException {
        flowExecutor.reloadRule();
        // 确保至少执行过3个不同的chain
        loadCache();
        // 随机执行chain
        loadCache(100);
        // 等待缓存淘汰
        Thread.sleep(200);
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

    // 测试chain被更新
    @Test
    public void testRuleCache3() throws InterruptedException {
        flowExecutor.reloadRule();
        loadCache();
        flowExecutor.execute2Resp("chain5", "arg");
        // chain1 被淘汰
        testEvicted("chain1");
        // 更新chain1
        LiteFlowChainELBuilder
                .createChain()
                .setChainId("chain1")
                .setEL("THEN(a, b, c)")
                .build();
        // 重新执行chain1
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>b==>c", response.getExecuteStepStr());
    }

    // 测试chain被移除
    @Test
    public void testRuleCache4() throws InterruptedException {
        flowExecutor.reloadRule();
        loadCache();
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>c", response.getExecuteStepStr());
        // chain1被淘汰
        testEvicted("chain1");
        // 手动移除chain5
        FlowBus.removeChain("chain5");
        response = flowExecutor.execute2Resp("chain5", "arg");
        Assertions.assertFalse(response.isSuccess());
        Assertions.assertEquals(ChainNotFoundException.class, response.getCause().getClass());
    }


    //  加载缓存
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
    private void testEvicted(String chanId) throws InterruptedException {
        Chain chain = FlowBus.getChain(chanId);
        int limit = 10; //  重试上限
        int count = 0;
        while (chain.isCompiled()) {
            // 等待 chain 被淘汰
            Thread.sleep(100);
            count++;
            if (count > limit) {
                throw new RuntimeException(chanId + " not be evicted");
            }
            System.out.println(count);
        }
        Assertions.assertNull(chain.getConditionList());
    }
}
