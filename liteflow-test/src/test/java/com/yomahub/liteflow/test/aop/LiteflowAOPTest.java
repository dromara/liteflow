package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ActiveProfiles("aop")
@SpringBootTest(classes = LiteflowAOPTest.class)
@EnableAutoConfiguration
@ComponentScan
public class LiteflowAOPTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testAop() throws Exception{
        LiteflowResponse<Slot> response= flowExecutor.execute("chain2", "it's a request");
        System.out.println(response);
    }
}
