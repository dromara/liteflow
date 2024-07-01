package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.el.NodeELWrapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = MaxWaitSecondBuilderTest.class)
@EnableAutoConfiguration
public class MaxWaitSecondBuilderTest {

    @Test
    public void testMaxWaitSecond1(){
        NodeELWrapper nodeA = ELBus.node("a").maxWaitSeconds(4);
        NodeELWrapper nodeB = ELBus.node("b").maxWaitSeconds(4);
        System.out.println(ELBus.when(nodeA, nodeB).toEL(true));

    }
}
