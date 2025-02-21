package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.*;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MaxWaitSecondBuilderTest.class)
@EnableAutoConfiguration
public class MaxWaitSecondBuilderTest extends BaseTest {

    // node层面
    @Test
    public void testMaxWaitSecond1(){
        CommonNodeELWrapper nodeA = ELBus.element("a").maxWaitSeconds(4);
        CommonNodeELWrapper nodeB = ELBus.element("b").maxWaitSeconds(4);
        WhenELWrapper whenELWrapper = ELBus.when(nodeA, nodeB);
        Assertions.assertEquals("WHEN(a.maxWaitSeconds(4),b.maxWaitSeconds(4));", whenELWrapper.toEL());
    }

    // when层面
    @Test
    public void testMaxWaitSecond2(){
        WhenELWrapper whenELWrapper = ELBus.when("a", "b").maxWaitSeconds(4);
        Assertions.assertEquals("WHEN(a,b).maxWaitSeconds(4);", whenELWrapper.toEL());
        ParELWrapper parELWrapper = ELBus.par("a", "b").maxWaitSeconds(4);
        Assertions.assertEquals("PAR(a,b).maxWaitSeconds(4);", parELWrapper.toEL());
    }

    // then层面
    @Test
    public void testMaxWaitSecond3(){
        ThenELWrapper thenELWrapper = ELBus.then("a", "b").maxWaitSeconds(4);
        Assertions.assertEquals("THEN(a,b).maxWaitSeconds(4);", thenELWrapper.toEL());
        SerELWrapper serELWrapper = ELBus.ser("a", "b").maxWaitSeconds(4);
        Assertions.assertEquals("SER(a,b).maxWaitSeconds(4);", serELWrapper.toEL());
    }

    // if层面
    @Test
    public void testMaxWaitSecond4(){
        IfELWrapper ifELWrapper = ELBus.ifOpt(ELBus.and("x1",ELBus.or("x2", "x3")), ELBus.then("a", "b"), ELBus.when("c", "d")).maxWaitSeconds(5);
        Assertions.assertEquals("IF(AND(x1,OR(x2,x3)),THEN(a,b),WHEN(c,d)).maxWaitSeconds(5);", ifELWrapper.toEL());
    }

    // switch层面
    @Test
    public void testMaxWaitSecond5(){
        SwitchELWrapper switchELWrapper = ELBus.switchOpt("a").to("b","c").maxWaitSeconds(5);
        Assertions.assertEquals("SWITCH(a).TO(b,c).maxWaitSeconds(5);", switchELWrapper.toEL());
    }

    // 循环层面
    @Test
    public void testMaxWaitSecond6(){
        LoopELWrapper forELWrapper = ELBus.forOpt(3).doOpt("a").maxWaitSeconds(5);
        Assertions.assertEquals("FOR(3).DO(a).maxWaitSeconds(5);", forELWrapper.toEL());
        LoopELWrapper whileELWrapper = ELBus.whileOpt("w").doOpt("a").maxWaitSeconds(5);
        Assertions.assertEquals("WHILE(w).DO(a).maxWaitSeconds(5);", whileELWrapper.toEL());
        LoopELWrapper iteratorELWrapper = ELBus.iteratorOpt("i").doOpt("a").maxWaitSeconds(5);
        Assertions.assertEquals("ITERATOR(i).DO(a).maxWaitSeconds(5);", iteratorELWrapper.toEL());
    }

}
