package com.yomahub.liteflow.test.preAndFinally;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境下pre节点和finally节点的测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/preAndFinally/application.properties")
public class PreAndFinallyELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试普通的pre和finally节点
    @Test
    public void testPreAndFinally1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("p1==>p2==>a==>b==>c==>f1==>f2",response.getExecuteStepStr());
    }

    //测试pre和finally节点不放在开头和结尾的情况
    @Test
    public void testPreAndFinally2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("p1==>p2==>a==>b==>c==>f1==>f2",response.getExecuteStepStr());
    }

    //测试有节点报错是否还执行finally节点的情况，其中d节点会报错，但依旧执行f1,f2节点
    @Test
    public void testPreAndFinally3() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals("p1==>p2==>a==>d==>f1==>f2", response.getExecuteStepStr());
    }

    //测试在finally节点里是否能获取exception
    @Test
    public void testPreAndFinally4() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertFalse(response.isSuccess());
        Assert.assertTrue(context.getData("hasEx"));
    }

    //测试嵌套结构pre和finally是否在各自的chain里打出
    @Test
    public void testPreAndFinally5() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("p1==>p2==>p1==>p2==>a==>b==>c==>f1==>f2==>f1", response.getExecuteStepStrWithoutTime());
    }

    //测试变量结构pre和finally是否在各自的chain里打出
    @Test
    public void testPreAndFinally6() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain6", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("p1==>p2==>p1==>p2==>a==>b==>c==>f1==>f2==>f1", response.getExecuteStepStrWithoutTime());
    }

    //测试el整体结构的多重pre和finally
    @Test
    public void testPreAndFinally7() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain7", "arg");
        Assert.assertTrue(response.isSuccess());
    }
}
