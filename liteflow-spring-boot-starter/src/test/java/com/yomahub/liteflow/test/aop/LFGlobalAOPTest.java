package com.yomahub.liteflow.test.aop;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.entity.data.Slot;
import com.yomahub.liteflow.test.aop.aspect.CmpAspect;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

/**
 * 切面场景单元测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@ActiveProfiles("aop")
@SpringBootTest(classes = LFGlobalAOPTest.class)
@EnableAutoConfiguration
@Import(CmpAspect.class)
@ComponentScan({"com.yomahub.liteflow.test.aop.cmp1","com.yomahub.liteflow.test.aop.cmp2"})
public class LFGlobalAOPTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试全局AOP，串行场景
    @Test
    public void testGlobalAopS() throws Exception{
        LiteflowResponse<Slot> response= flowExecutor.execute("chain1", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", response.getData().getData("a"));
        Assert.assertEquals("before_after", response.getData().getData("b"));
        Assert.assertEquals("before_after", response.getData().getData("c"));
        Assert.assertEquals("before_after", response.getData().getData("d"));
        Assert.assertEquals("before_after", response.getData().getData("e"));
    }

    //测试全局AOP，并行场景
    @Test
    public void testGlobalAopP() throws Exception{
        LiteflowResponse<Slot> response= flowExecutor.execute("chain2", "it's a request");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("before_after", response.getData().getData("a"));
        Assert.assertEquals("before_after", response.getData().getData("b"));
        Assert.assertEquals("before_after", response.getData().getData("c"));
        Assert.assertEquals("before_after", response.getData().getData("d"));
        Assert.assertEquals("before_after", response.getData().getData("e"));
    }
}
