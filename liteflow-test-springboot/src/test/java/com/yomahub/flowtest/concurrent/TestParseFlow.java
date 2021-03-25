package com.yomahub.flowtest.concurrent;


import com.yomahub.liteflow.entity.flow.Chain;
import com.yomahub.liteflow.entity.flow.Condition;
import com.yomahub.liteflow.entity.flow.ThenCondition;
import com.yomahub.liteflow.entity.flow.WhenCondition;
import com.yomahub.liteflow.flow.FlowBus;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;


/**
 * desc :
 * name : TestParseFlow
 *
 * @author : xujia
 * date : 2021/3/25
 * @since : 1.8
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestParseFlow {

    private Check caseAsync = new Check("async", Arrays.asList(
            new AbstractMap.SimpleEntry<>(ThenCondition.class, null),
            new AbstractMap.SimpleEntry<>(WhenCondition.class, false),
            new AbstractMap.SimpleEntry<>(WhenCondition.class, true),
            new AbstractMap.SimpleEntry<>(WhenCondition.class, true)
    ));

    private Check caseConcurrent = new Check("async-concurrent1", Arrays.asList(
            new AbstractMap.SimpleEntry<>(WhenCondition.class, true)
    ));

    @Test
    public void parseWhen() throws Exception {
        assertTrue(caseAsync, FlowBus.getChain(caseAsync.getChainCode()));

        assertTrue(caseConcurrent, FlowBus.getChain(caseConcurrent.getChainCode()));
    }

    private void assertTrue(Check check, Chain chain) {
        Assert.assertNotNull(chain);

        Assert.assertTrue(null != chain.getConditionList() && !chain.getConditionList().isEmpty());
        for (int i = 0; i < chain.getConditionList().size(); i ++) {

            AbstractMap.SimpleEntry<Class<?>, Boolean> expected = check.getAsyncWithWhen().get(i);
            Condition actual = chain.getConditionList().get(i);

            Assert.assertEquals(expected.getKey(), actual.getClass());
            if (actual.getClass().equals(WhenCondition.class)) {
                Assert.assertEquals(expected.getValue(), ((WhenCondition) actual).isASync());
            }
        }
    }

    public static class Check {
        private String chainCode;
        private List<AbstractMap.SimpleEntry<Class<?>, Boolean>> asyncWithWhen;

        public Check(String chainCode, List<AbstractMap.SimpleEntry<Class<?>, Boolean>> asyncWithWhen) {
            this.chainCode = chainCode;
            this.asyncWithWhen = asyncWithWhen;
        }

        public String getChainCode() {
            return chainCode;
        }

        public List<AbstractMap.SimpleEntry<Class<?>, Boolean>> getAsyncWithWhen() {
            return asyncWithWhen;
        }
    }
}
