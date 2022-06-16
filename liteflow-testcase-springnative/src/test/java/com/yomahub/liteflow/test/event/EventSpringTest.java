package com.yomahub.liteflow.test.event;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/event/application.xml")
public class EventSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试组件成功事件
    @Test
    public void testEvent1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("abc", context.getData("test"));
    }

    //测试组件失败事件
    @Test
    public void testEvent2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(NullPointerException.class, response.getCause().getClass());
        Assert.assertEquals("ab", context.getData("test"));
        Assert.assertEquals("error:d", context.getData("error"));
    }

    //测试组件失败事件本身抛出异常
    @Test
    public void testEvent3() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertFalse(response.isSuccess());
        Assert.assertEquals(NullPointerException.class, response.getCause().getClass());
        Assert.assertEquals("a", context.getData("test"));
        Assert.assertEquals("error:e", context.getData("error"));
    }
}
