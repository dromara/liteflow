package com.yomahub.liteflow.benchmark;

import cn.hutool.core.date.StopWatch;
import cn.hutool.core.util.StrUtil;
import com.yomahub.liteflow.benchmark.context.BatchMessageResultContext;
import com.yomahub.liteflow.builder.el.LiteFlowChainELBuilder;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
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
@SpringBootTest(classes = TestCompile.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.benchmark.cmp" })
public class TestCompile {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void test1() throws Exception {
        LiteflowResponse response = flowExecutor.execute2Resp("channelSenderChain", null, BatchMessageResultContext.class);
        BatchMessageResultContext context = response.getFirstContextBean();
        if (response.isSuccess()){
            System.out.println("执行成功，最终选择的渠道是:" + context.getFinalResultChannel());
        }else{
            System.out.println("执行失败:" + response.getCause());
        }
    }

    @Test
    public void test2() throws Exception {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        String el = "selectBestChannel = THEN(WHEN(channel1Query, channel2Query, channel3Query,channel4Query, channel5Query, channel6Query),channelSelector).id(\"branch1\");\n" +
                "        selectBizChannel = THEN(biz1,SWITCH(if_2).to(channel3,channel4,SWITCH(if_3).to(channel5, channel6).id(\"s3\")).id(\"s2\")).id(\"branch2\");\n" +
                "        THEN(packageData.tag(\"{}\"),SWITCH(if_1).to(channel1,channel2,selectBestChannel,selectBizChannel),batchSender);";

        for (int i = 1; i <= 20000; i++) {
            LiteFlowChainELBuilder.createChain().setChainId("chain_build_"+i).setEL(StrUtil.format(el,i)).build();
        }

        stopWatch.stop();

        System.out.println(StrUtil.format("耗时:{}",stopWatch.getTotalTimeMillis()));

    }
}
