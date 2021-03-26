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

    private Check caseAsync = new Check("async", Arrays.asList(
            ThenCondition.class,
            WhenCondition.class,
            WhenCondition.class,
            WhenCondition.class
    ));

    private Check caseConcurrent = new Check("async-concurrent1", Collections.singletonList(
            WhenCondition.class
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

            Class<? extends Condition> expected = check.getConditionClazz().get(i);
            Condition actual = chain.getConditionList().get(i);

            Assert.assertEquals(expected, actual.getClass());
        }
    }

    public static class Check {
        private String chainCode;
        private List<Class<? extends Condition>> conditionClazz;

        public Check(String chainCode, List<Class<? extends Condition>> conditionClazz) {
            this.chainCode = chainCode;
            this.conditionClazz = conditionClazz;
        }

        public String getChainCode() {
            return chainCode;
        }

        public List<Class<? extends Condition>> getConditionClazz() {
            return conditionClazz;
        }
    }
}
