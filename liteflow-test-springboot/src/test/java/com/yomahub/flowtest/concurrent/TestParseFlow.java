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
import java.util.Collections;
import java.util.List;


/**
 * 测试流程的解析
 * @author justin.xu
 */
@ActiveProfiles("test")
@RunWith(SpringRunner.class)
@SpringBootTest
public class TestParseFlow {

    private Check caseErrorResume = new Check("test-errorResume", Arrays.asList(
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(ThenCondition.class, null),
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(WhenCondition.class, true),
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(WhenCondition.class, true),
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(WhenCondition.class, true)
    ));

    private Check caseErrorBreak = new Check("test-errorBreak", Arrays.asList(
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(ThenCondition.class, null),
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(WhenCondition.class, true),
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(WhenCondition.class, false),
            new AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>(WhenCondition.class, true)
    ));

    @Test
    public void parseWhen() throws Exception {
        assertTrue(caseErrorResume, FlowBus.getChain(caseErrorResume.getChainCode()));

        assertTrue(caseErrorBreak, FlowBus.getChain(caseErrorBreak.getChainCode()));
    }

    private void assertTrue(Check check, Chain chain) {
        Assert.assertNotNull(chain);

        Assert.assertTrue(null != chain.getConditionList() && !chain.getConditionList().isEmpty());
        for (int i = 0; i < chain.getConditionList().size(); i ++) {

            AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean> expected = check.getClazzWithFlags().get(i);
            Condition actual = chain.getConditionList().get(i);

            Assert.assertEquals(expected.getKey(), actual.getClass());
            if (actual.getClass().equals(WhenCondition.class)) {
                Assert.assertEquals(expected.getValue(), ((WhenCondition) actual).isErrorResume());
            }
        }
    }

    public static class Check {
        private String chainCode;
        private List<AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>> clazzWithFlags;

        public Check(String chainCode, List<AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>> clazzWithFlags) {
            this.chainCode = chainCode;
            this.clazzWithFlags = clazzWithFlags;
        }

        public String getChainCode() {
            return chainCode;
        }

        public List<AbstractMap.SimpleEntry<Class<? extends Condition>, Boolean>> getClazzWithFlags() {
            return clazzWithFlags;
        }
    }
}
