package com.yomahub.liteflow.benchmark;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.benchmark.bean.PriceCalcReqVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.util.JsonUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import javax.annotation.Resource;

@ExtendWith(SpringExtension.class)
@TestPropertySource(value = "classpath:application.properties")
@SpringBootTest(classes = CommonExampleTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.benchmark.cmp" })
public class CommonExampleTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void test1() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        PriceCalcReqVO req = JsonUtil.parseObject(ResourceUtil.readUtf8Str("reqData.json"), PriceCalcReqVO.class);
        for (int i = 0; i < 60000; i++) {
            LiteflowResponse response = flowExecutor.execute2Resp("mainChain", req, PriceContext.class);
            if (!response.isSuccess()){
                throw response.getCause();
            }
        }
        stopWatch.stop();
        System.out.println(StrUtil.format("耗时:{}",stopWatch.getTotalTimeMillis()));
    }

    @Test
    public void test2() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        String el = "THEN(\n" +
                "            checkCmp, slotInitCmp, priceStepInitCmp,\n" +
                "            promotionConvertCmp, s_memberDiscountCmp,\n" +
                "            promotionChain, s_couponCmp,\n" +
                "            SWITCH(postageCondCmp).to(postageCmp, overseaPostageCmp),\n" +
                "            priceResultCmp, stepPrintCmp\n" +
                "        );";

        for (int i = 0; i < 20000; i++) {
            LiteFlowChainELBuilder.createChain().setChainId("chain_build_" + i).setEL(el).build();
        }
        stopWatch.stop();
        System.out.println(StrUtil.format("耗时:{}",stopWatch.getTotalTimeMillis()));
    }

    @Test
    public void test3() throws Exception {
        String el = "THEN(\n" +
                "            checkCmp.tag(\"{}\"), slotInitCmp, priceStepInitCmp,\n" +
                "            promotionConvertCmp, s_memberDiscountCmp,\n" +
                "            promotionChain, s_couponCmp,\n" +
                "            SWITCH(postageCondCmp).to(postageCmp, overseaPostageCmp),\n" +
                "            priceResultCmp, stepPrintCmp\n" +
                "        );";
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        for (int i = 0; i < 20000; i++) {
            LiteFlowChainELBuilder.createChain().setChainId("chain_build_" + i).setEL(StrUtil.format(el, i)).build();
        }
        stopWatch.stop();
        System.out.println(StrUtil.format("耗时:{}，加载规则总数:{}",stopWatch.getTotalTimeMillis(), FlowBus.getChainMap().size()));
    }
}
