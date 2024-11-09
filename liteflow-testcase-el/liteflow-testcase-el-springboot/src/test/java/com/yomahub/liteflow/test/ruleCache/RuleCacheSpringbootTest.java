package com.yomahub.liteflow.test.ruleCache;

import cn.hutool.core.collection.CollUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.flow.element.Chain;
import com.yomahub.liteflow.flow.element.Condition;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.rollback.RollbackSpringbootTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;

import javax.annotation.Resource;
import java.util.List;
import java.util.Random;

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
        // 加满缓存
        loadCache();
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        // chain1 被淘汰
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("x==>a==>b", response.getExecuteStepStr());
        testEvicted("chain1");
    }

    // 测试chain被手动移除
    @Test
    public void testRuleCache2() throws InterruptedException {
        // 随机执行chain
        loadCache(100);
        Thread.sleep(100);
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
