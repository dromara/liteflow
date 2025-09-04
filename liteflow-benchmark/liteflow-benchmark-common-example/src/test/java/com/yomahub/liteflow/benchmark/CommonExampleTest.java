package com.yomahub.liteflow.benchmark;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.benchmark.bean.PriceCalcReqVO;
import com.yomahub.liteflow.benchmark.context.PriceContext;
import com.yomahub.liteflow.core.FlowExecutor;
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
        PriceCalcReqVO req = JsonUtil.parseObject(ResourceUtil.readUtf8Str("reqData.json"), PriceCalcReqVO.class);
        LiteflowResponse response = flowExecutor.execute2Resp("mainChain", req, PriceContext.class);
        if (!response.isSuccess()){
            throw response.getCause();
        }
    }
}
