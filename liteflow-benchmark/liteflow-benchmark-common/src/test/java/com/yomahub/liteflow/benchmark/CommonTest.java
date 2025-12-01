package com.yomahub.liteflow.benchmark;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.benchmark.cmp.TestContext;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
@SpringBootTest(classes = CommonTest.class)
@EnableAutoConfiguration
@ComponentScan({ "com.yomahub.liteflow.benchmark.cmp" })
public class CommonTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void test1() throws Exception {
        TestContext context = new TestContext();
        context.setName("tom");
        context.setAge(21);
        DefaultContext defaultContext = new DefaultContext();

        LiteflowResponse response = flowExecutor.execute2Resp("chain1", null, context,defaultContext);
        if (!response.isSuccess()){
            throw response.getCause();
        }
    }
}
