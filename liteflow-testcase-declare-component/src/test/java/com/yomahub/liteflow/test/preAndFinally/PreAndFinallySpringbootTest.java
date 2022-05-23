package com.yomahub.liteflow.test.preAndFinally;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.slot.DefaultSlot;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * springboot环境下pre节点和finally节点的测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/preAndFinally/application.properties")
@SpringBootTest(classes = PreAndFinallySpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.preAndFinally.cmp"})
public class PreAndFinallySpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试普通的pre和finally节点
    @Test
    public void testPreAndFinally1() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("p1==>p2==>a==>b==>c==>f1==>f2",response.getSlot().getExecuteStepStr());
    }

    //测试pre和finally节点不放在开头和结尾的情况
    @Test
    public void testPreAndFinally2() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("p1==>p2==>a==>b==>c==>f1==>f2",response.getSlot().getExecuteStepStr());
    }

    //测试有节点报错是否还执行finally节点的情况，其中d节点会报错，但依旧执行f1,f2节点
    @Test
    public void testPreAndFinally3() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("p1==>p2==>a==>d==>f1==>f2", response.getSlot().getExecuteStepStr());
    }

    //测试在finally节点里是否能获取exception
    @Test
    public void testPreAndFinally4() throws Exception{
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(response.getSlot().getData("hasEx"));
    }
}
