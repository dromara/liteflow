package com.yomahub.liteflow.test.builder;

import cn.hutool.core.date.DateUtil;
import com.yomahub.liteflow.builder.LiteFlowNodeBuilder;
import com.yomahub.liteflow.builder.el.ELBus;
import com.yomahub.liteflow.builder.el.ELWrapper;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.builder.el.ThenELWrapper;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.NodeTypeEnum;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import com.yomahub.liteflow.test.builder.cmp.ACmp;
import com.yomahub.liteflow.test.builder.cmp.BCmp;
import com.yomahub.liteflow.test.builder.vo.User;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.Resource;

/**
 * 复杂编排例子测试
 *
 * @author gezuao
 * @since 2.11.1
 */
@SpringBootTest(classes = ComplexELBuilderTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.test.builder.ComplexELBuilderTest" })
public class ComplexELBuilderTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

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
        ThenELWrapper complexEl = ELBus.then(
                ELBus.element("A"),
                ELBus.when(
                        ELBus.then(ELBus.element("B"), ELBus.element("C")),
                        ELBus.then(ELBus.element("D")).then(ELBus.element("E")).then(ELBus.element("F")),
                        ELBus.then(
                                ELBus.switchOpt(ELBus.element("G")).to(
                                        ELBus.then(ELBus.element("H"), ELBus.element("I"), ELBus.when(ELBus.element("J")).when(ELBus.element("K"))).id("t1"),
                                        ELBus.then(ELBus.element("L"), ELBus.element("M")).id("t2")
                                ),
                                ELBus.element("N")
                        )
                ),
                ELBus.element("Z")
        );

        System.out.println(StringEscapeUtils.escapeJava(complexEl.toEL()));
        String expect1 = "THEN(A,WHEN(THEN(B,C),THEN(D,E,F),THEN(SWITCH(G).TO(THEN(H,I,WHEN(J,K)).id(\"t1\"),THEN(L,M).id(\"t2\")),N)),Z);";
        Assertions.assertEquals(expect1, complexEl.toEL());

        System.out.println(StringEscapeUtils.escapeJava(complexEl.toEL(true)));
        String expect2 = "THEN(\n\tA,\n\tWHEN(\n\t\tTHEN(\n\t\t\tB,\n\t\t\tC\n\t\t),\n\t\tTHEN(\n\t\t\tD,\n\t\t\tE,\n\t\t\tF\n\t\t),\n\t\tTHEN(\n\t\t\tSWITCH(G).TO(\n\t\t\t\tTHEN(\n\t\t\t\t\tH,\n\t\t\t\t\tI,\n\t\t\t\t\tWHEN(\n\t\t\t\t\t\tJ,\n\t\t\t\t\t\tK\n\t\t\t\t\t)\n\t\t\t\t).id(\"t1\"),\n\t\t\t\tTHEN(\n\t\t\t\t\tL,\n\t\t\t\t\tM\n\t\t\t\t).id(\"t2\")\n\t\t\t),\n\t\t\tN\n\t\t)\n\t),\n\tZ\n);";
        Assertions.assertEquals(expect2, complexEl.toEL(true));
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
        ThenELWrapper complexEl = ELBus.then(
                ELBus.element("A"),
                ELBus.switchOpt(ELBus.element("B")).to(
                        ELBus.then(ELBus.element("D"),ELBus.element("E")).then(ELBus.element("F")).id("t1"),
                        ELBus.then(
                                ELBus.element("C"),
                                ELBus.when(
                                        ELBus.then(
                                                ELBus.switchOpt(ELBus.element("G")).to(
                                                        ELBus.then(ELBus.element("H"), ELBus.element("I")).id("t2"),
                                                        ELBus.element("J")
                                                ),
                                                ELBus.element("K")
                                        ),
                                        ELBus.then(ELBus.element("L"), ELBus.element("M"))
                                )
                        ).id("t3")
                ),
                ELBus.element("Z")
        );

        System.out.println(StringEscapeUtils.escapeJava(complexEl.toEL()));
        String expect1 = "THEN(A,SWITCH(B).TO(THEN(D,E,F).id(\"t1\"),THEN(C,WHEN(THEN(SWITCH(G).TO(THEN(H,I).id(\"t2\"),J),K),THEN(L,M))).id(\"t3\")),Z);";
        Assertions.assertEquals(expect1, complexEl.toEL());

        System.out.println(StringEscapeUtils.escapeJava(complexEl.toEL(true)));
        String expect2 = "THEN(\n\tA,\n\tSWITCH(B).TO(\n\t\tTHEN(\n\t\t\tD,\n\t\t\tE,\n\t\t\tF\n\t\t).id(\"t1\"),\n\t\tTHEN(\n\t\t\tC,\n\t\t\tWHEN(\n\t\t\t\tTHEN(\n\t\t\t\t\tSWITCH(G).TO(\n\t\t\t\t\t\tTHEN(\n\t\t\t\t\t\t\tH,\n\t\t\t\t\t\t\tI\n\t\t\t\t\t\t).id(\"t2\"),\n\t\t\t\t\t\tJ\n\t\t\t\t\t),\n\t\t\t\t\tK\n\t\t\t\t),\n\t\t\t\tTHEN(\n\t\t\t\t\tL,\n\t\t\t\t\tM\n\t\t\t\t)\n\t\t\t)\n\t\t).id(\"t3\")\n\t),\n\tZ\n);";
        Assertions.assertEquals(expect2, complexEl.toEL(true));
    }

    /**
     * 创建Node，创建EL表达式，创建Chain
     * 执行Chain，校验Data参数
     */
    @Test
    public void test3(){
        LiteFlowNodeBuilder.createNode()
                .setId("a")
                .setName("组件A")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(ACmp.class)
                .build();
        LiteFlowNodeBuilder.createNode()
                .setId("b")
                .setName("组件B")
                .setType(NodeTypeEnum.COMMON)
                .setClazz(BCmp.class)
                .build();

        ELWrapper elWrapper = ELBus.then(ELBus.element("a").data("sql", "select * from member t where t.id=10001"),
                ELBus.element("b").data("cmpData", "{\"name\":\"jack\",\"age\":27,\"birth\":\"1995-10-01\"}"));

        System.out.println(elWrapper.toEL());

        String expectStr = "sql = \"select * from member t where t.id=10001\";\n" +
                "                cmpData = \"{\\\"name\\\":\\\"jack\\\",\\\"age\\\":27,\\\"birth\\\":\\\"1995-10-01\\\"}\";\n" +
                "\n" +
                "        THEN(\n" +
                "                a.data(sql),\n" +
                "                b.data(cmpData)\n" +
                "        );";


        Assertions.assertTrue(LiteFlowChainELBuilder.validate(expectStr));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(elWrapper.toEL()));
        Assertions.assertTrue(LiteFlowChainELBuilder.validate(elWrapper.toEL(true)));
        LiteFlowChainELBuilder.createChain().setChainId("chain1").setEL(
                elWrapper.toEL(true)
        ).build();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assertions.assertTrue(response.isSuccess());

        DefaultContext context = response.getFirstContextBean();
        User user = context.getData("user");
        Assertions.assertEquals(27, user.getAge());
        Assertions.assertEquals("jack", user.getName());
        Assertions.assertEquals(0, user.getBirth().compareTo(DateUtil.parseDate("1995-10-01").toJdkDate()));
    }
}
