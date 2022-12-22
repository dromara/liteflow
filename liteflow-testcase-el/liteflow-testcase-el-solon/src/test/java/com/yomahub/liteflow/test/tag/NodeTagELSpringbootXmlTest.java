package com.yomahub.liteflow.test.tag;

import cn.hutool.core.collection.ConcurrentHashSet;
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
 * springboot环境下隐私投递的测试
 * @author Bryan.Zhang
 * @since 2.5.0
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/tag/application-xml.properties")
public class NodeTagELSpringbootXmlTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    @Test
    public void testTag1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("123",context.getData("test"));
    }

    @Test
    public void testTag2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("a==>a==>a==>c==>e", response.getExecuteStepStr());
    }

    //测试多线程when情况下的tag取值是否正确
    //这里循环多次的原因是，因为when多线程，有时候因为凑巧，可能正确。所以多次情况下在2.6.4版本肯定出错
    @Test
    public void testTag3() throws Exception{
        for (int i = 0; i < 50; i++) {
            LiteflowResponse response = flowExecutor.execute2Resp("chain3", "arg");
            DefaultContext context = response.getFirstContextBean();
            Assert.assertTrue(response.isSuccess());
            ConcurrentHashSet<String> testSet = context.getData("test");
            Assert.assertEquals(3, testSet.size());
        }
    }

    //测试tag是否能在isAccess中起效
    @Test
    public void testTag4() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain4", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("g", response.getExecuteStepStr());
    }

    //测试tag是否能在WHEN中起效果
    @Test
    public void testTag5() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain5", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("1",context.getData("test"));
    }
}
