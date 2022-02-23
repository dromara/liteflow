package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.spring.ComponentScanner;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 切面场景单元测试
 * @author Bryan.Zhang
 */

@RunWith(SpringRunner.class)
@ContextConfiguration("classpath:/aop/application-global.xml")
public class GlobalAOPSpringTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试全局AOP，串行场景
    @Test
    public void testGlobalAopS() {
        LiteflowResponse<DefaultSlot> response= flowExecutor.execute2Resp("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", response.getSlot().getData("a"));
        Assert.assertEquals("before_after", response.getSlot().getData("b"));
        Assert.assertEquals("before_after", response.getSlot().getData("c"));
        Assert.assertEquals("before_after", response.getSlot().getData("d"));
        Assert.assertEquals("before_after", response.getSlot().getData("e"));
    }

    //测试全局AOP，并行场景
    @Test
    public void testGlobalAopP() {
        LiteflowResponse<DefaultSlot> response= flowExecutor.execute2Resp("chain2", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", response.getSlot().getData("a"));
        Assert.assertEquals("before_after", response.getSlot().getData("b"));
        Assert.assertEquals("before_after", response.getSlot().getData("c"));
        Assert.assertEquals("before_after", response.getSlot().getData("d"));
        Assert.assertEquals("before_after", response.getSlot().getData("e"));
    }

    @AfterClass
    public static void cleanScanCache(){
        BaseTest.cleanScanCache();
        ComponentScanner.cmpAroundAspect = null;
    }
}
