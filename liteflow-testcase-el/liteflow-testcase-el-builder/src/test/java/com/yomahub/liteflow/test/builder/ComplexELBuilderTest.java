package com.yomahub.liteflow.test.builder;

import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.ThenELWrapper;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 复杂编排例子测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = ComplexELBuilderTest.class)
@EnableAutoConfiguration
public class ComplexELBuilderTest extends BaseTest {
    /*
    复杂编排例子1
    THEN(
        A,
        WHEN(
            THEN(B, C),
            THEN(D, E, F),
            THEN(
                SWITCH(G).to(
                    THEN(H, I, WHEN(J, K)).id("t1"),
                    THEN(L, M).id("t2")
                ),
                N
            )
        ),
        Z
    );
     */
    @Test
    public void testComplexEL1() {
        ThenELWrapper complexEl1 = ELBus.then(
                ELBus.node("A"),
                ELBus.when(
                        ELBus.then("B", "C"),
                        ELBus.then(ELBus.node("D")).then("E").then("F"),
                        ELBus.then(
                                ELBus.switchOpt("G").to(
                                        ELBus.then("H", ELBus.node("I"), ELBus.when("J").when("K")).id("t1"),
                                        ELBus.then("L", "M").id("t2")
                                ),
                                "N"
                        )
                ),
                "Z"
        );
        String expectedStr = "THEN(node(\"A\"),WHEN(THEN(node(\"B\"),node(\"C\")),THEN(node(\"D\"),node(\"E\"),node(\"F\")),THEN(SWITCH(node(\"G\")).TO(THEN(node(\"H\"),node(\"I\"),WHEN(node(\"J\"),node(\"K\"))).id(\"t1\"),THEN(node(\"L\"),node(\"M\")).id(\"t2\")),node(\"N\"))),node(\"Z\"))";
        Assertions.assertEquals(expectedStr,
                complexEl1.toEL());
        System.out.println(expectedStr);

        expectedStr = "THEN(\n\tnode(\"A\"),\n\tWHEN(\n\t\tTHEN(\n\t\t\tnode(\"B\"),\n\t\t\tnode(\"C\")\n\t\t),\n\t\tTHEN(\n\t\t\tnode(\"D\"),\n\t\t\tnode(\"E\"),\n\t\t\tnode(\"F\")\n\t\t),\n\t\tTHEN(\n\t\t\tSWITCH(node(\"G\")).TO(\n\t\t\t\tTHEN(\n\t\t\t\t\tnode(\"H\"),\n\t\t\t\t\tnode(\"I\"),\n\t\t\t\t\tWHEN(\n\t\t\t\t\t\tnode(\"J\"),\n\t\t\t\t\t\tnode(\"K\")\n\t\t\t\t\t)\n\t\t\t\t).id(\"t1\"),\n\t\t\t\tTHEN(\n\t\t\t\t\tnode(\"L\"),\n\t\t\t\t\tnode(\"M\")\n\t\t\t\t).id(\"t2\")\n\t\t\t),\n\t\t\tnode(\"N\")\n\t\t)\n\t),\n\tnode(\"Z\")\n)";
        Assertions.assertEquals(expectedStr,
                complexEl1.toEL(true));
        System.out.println(expectedStr);

    }

    /*
    复杂编排例子2
    THEN(
        A,
        SWITCH(B).to(
            THEN(D, E, F).id("t1"),
            THEN(
                C,
                WHEN(
                    THEN(
                        SWITCH(G).to(THEN(H, I).id("t2"), J),
                        K
                    ),
                    THEN(L, M)
                )
            ).id("t3")
        ),
        Z
    );
     */
    @Test
    public void testComplexEl2(){
        ThenELWrapper complexEl2 = ELBus.then(
                ELBus.node("A"),
                ELBus.switchOpt(ELBus.node("B")).to(
                        ELBus.then("D","E").then(ELBus.node("F")).id("t1"),
                        ELBus.then(
                                ELBus.node("C"),
                                ELBus.when(
                                        ELBus.then(
                                                ELBus.switchOpt("G").to(
                                                        ELBus.then("H", "I").id("t2"),
                                                        "J"
                                                ),
                                                "K"
                                        ),
                                        ELBus.then("L", "M")
                                )
                        ).id("t3")
                ),
                ELBus.node("Z")
        );
        String expectedStr = "THEN(node(\"A\"),SWITCH(node(\"B\")).TO(THEN(node(\"D\"),node(\"E\"),node(\"F\")).id(\"t1\"),THEN(node(\"C\"),WHEN(THEN(SWITCH(node(\"G\")).TO(THEN(node(\"H\"),node(\"I\")).id(\"t2\"),node(\"J\")),node(\"K\")),THEN(node(\"L\"),node(\"M\")))).id(\"t3\")),node(\"Z\"))";
        Assertions.assertEquals(expectedStr,
                complexEl2.toEL());
        System.out.println(expectedStr);

        expectedStr = "THEN(\n\tnode(\"A\"),\n\tSWITCH(node(\"B\")).TO(\n\t\tTHEN(\n\t\t\tnode(\"D\"),\n\t\t\tnode(\"E\"),\n\t\t\tnode(\"F\")\n\t\t).id(\"t1\"),\n\t\tTHEN(\n\t\t\tnode(\"C\"),\n\t\t\tWHEN(\n\t\t\t\tTHEN(\n\t\t\t\t\tSWITCH(node(\"G\")).TO(\n\t\t\t\t\t\tTHEN(\n\t\t\t\t\t\t\tnode(\"H\"),\n\t\t\t\t\t\t\tnode(\"I\")\n\t\t\t\t\t\t).id(\"t2\"),\n\t\t\t\t\t\tnode(\"J\")\n\t\t\t\t\t),\n\t\t\t\t\tnode(\"K\")\n\t\t\t\t),\n\t\t\t\tTHEN(\n\t\t\t\t\tnode(\"L\"),\n\t\t\t\t\tnode(\"M\")\n\t\t\t\t)\n\t\t\t)\n\t\t).id(\"t3\")\n\t),\n\tnode(\"Z\")\n)";
        Assertions.assertEquals(expectedStr,
                complexEl2.toEL(true));
        System.out.println(expectedStr);
    }

}
