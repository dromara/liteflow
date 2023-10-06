package com.yomahub.liteflow.test.fallback;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.core.FlowExecutorHolder;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.property.LiteflowConfig;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 非 Spring 环境下组件降级测试
 *
 * @author DaleLee
 * @since 2.11.1
 */
public class FallbackTest extends BaseTest {
    private static FlowExecutor flowExecutor;

    @BeforeAll
    public static void init() {
        LiteflowConfig config = new LiteflowConfig();
        config.setRuleSource("fallback/flow.el.xml");
        config.setFallbackCmpEnable(true);
        flowExecutor = FlowExecutorHolder.loadInstance(config);
    }

    @Test
    public void testThen1() {
        LiteflowResponse response = flowExecutor.execute2Resp("then1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testThen2() {
        LiteflowResponse response = flowExecutor.execute2Resp("then2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("c==>c==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testWhen1() {
        LiteflowResponse response = flowExecutor.execute2Resp("when1", "arg");
        Assertions.assertTrue(response.isSuccess());
        String executeStepStr = response.getExecuteStepStrWithoutTime();
        Assertions.assertTrue("b==>c".equals(executeStepStr) || "c==>b".equals(executeStepStr));
    }

    @Test
    public void testIf1() {
        LiteflowResponse response = flowExecutor.execute2Resp("if1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ifn2", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testIf2() {
        LiteflowResponse response = flowExecutor.execute2Resp("if2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ifn1==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testFor1() {
        LiteflowResponse response = flowExecutor.execute2Resp("for1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("for1==>a==>a==>a", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testFor2() {
        LiteflowResponse response = flowExecutor.execute2Resp("for2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("LOOP_3==>c==>c==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testWhile1() {
        LiteflowResponse response = flowExecutor.execute2Resp("while1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("wn2", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testWhile2() {
        LiteflowResponse response = flowExecutor.execute2Resp("while2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("wn1==>c==>wn1==>c==>wn1==>c==>wn1", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testIterator1() {
        LiteflowResponse response = flowExecutor.execute2Resp("iterator1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("itn2", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testIterator2() {
        LiteflowResponse response = flowExecutor.execute2Resp("iterator2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("itn1==>c==>c==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testBreak1() {
        LiteflowResponse response = flowExecutor.execute2Resp("break1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("LOOP_3==>a==>bn1", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testBreak2() {
        LiteflowResponse response = flowExecutor.execute2Resp("break2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("wn1==>a==>bn1", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testBreak3() {
        LiteflowResponse response = flowExecutor.execute2Resp("break3", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("itn1==>a==>bn1", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testSwitch1() {
        LiteflowResponse response = flowExecutor.execute2Resp("switch1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("swn2==>b", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testSwitch2() {
        LiteflowResponse response = flowExecutor.execute2Resp("switch2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("swn1==>a", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testAnd1() {
        LiteflowResponse response = flowExecutor.execute2Resp("and1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ifn2", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testOr1() {
        LiteflowResponse response = flowExecutor.execute2Resp("or1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ifn2==>ifn1==>a", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testNot1() {
        LiteflowResponse response = flowExecutor.execute2Resp("not1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ifn2==>a", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testCatch1() {
        LiteflowResponse response = flowExecutor.execute2Resp("catch1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>d==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testMulti1() {
        LiteflowResponse response = flowExecutor.execute2Resp("multi1", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("a==>c==>ifn2", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testMulti2() {
        LiteflowResponse response = flowExecutor.execute2Resp("multi2", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("ifn2==>ifn1==>a==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testMulti3() {
        LiteflowResponse response = flowExecutor.execute2Resp("multi3", "arg");
        Assertions.assertTrue(response.isSuccess());
        Assertions.assertEquals("for1==>b==>c==>b==>c==>b==>c", response.getExecuteStepStrWithoutTime());
    }

    @Test
    public void testConcurrent1() {
        LiteflowResponse response = flowExecutor.execute2Resp("concurrent1", "arg");
        Assertions.assertTrue(response.isSuccess());
        String stepStr = response.getExecuteStepStrWithoutTime();
        Assertions.assertTrue("c==>ifn2".equals(stepStr) || "ifn2==>c".equals(stepStr));
    }

    @Test
    public void testConcurrent2() {
        LiteflowResponse response = flowExecutor.execute2Resp("concurrent2", "arg");
        Assertions.assertTrue(response.isSuccess());
        String stepStr = response.getExecuteStepStrWithoutTime();
        Assertions.assertTrue("c==>ifn2".equals(stepStr) || "ifn2==>c".equals(stepStr));
    }

    @Test
    public void testConcurrent3() throws ExecutionException, InterruptedException {
        // 执行多条 chain
        Future<LiteflowResponse> future1 = flowExecutor.execute2Future("concurrent1", "arg", new Object());
        Future<LiteflowResponse> future2 = flowExecutor.execute2Future("concurrent2", "arg", new Object());
        Thread.sleep(1000);
        LiteflowResponse response1 = future1.get();
        LiteflowResponse response2 = future2.get();
        Assertions.assertTrue(response1.isSuccess());
        String stepStr1 = response1.getExecuteStepStrWithoutTime();
        Assertions.assertTrue("c==>ifn2".equals(stepStr1) || "ifn2==>c".equals(stepStr1));
        Assertions.assertTrue(response2.isSuccess());
        String stepStr2 = response2.getExecuteStepStrWithoutTime();
        Assertions.assertTrue("c==>ifn2".equals(stepStr2) || "ifn2==>c".equals(stepStr2));
    }
}
