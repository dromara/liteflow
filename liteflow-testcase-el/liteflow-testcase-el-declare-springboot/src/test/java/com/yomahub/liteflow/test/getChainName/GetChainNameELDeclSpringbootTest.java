package com.yomahub.liteflow.test.getChainName;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.slot.DefaultContext;
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
 * springboot环境获取ChainName的测试
 * @author Bryan.Zhang
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/getChainName/application.properties")
@SpringBootTest(classes = GetChainNameELDeclSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.getChainName.cmp"})
public class GetChainNameELDeclSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testGetChainName1() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("sub1", context.getData("a"));
        Assert.assertEquals("sub2", context.getData("b"));
        Assert.assertEquals("sub3", context.getData("c"));
        Assert.assertEquals("sub4", context.getData("d"));
    }

    @Test
    public void testGetChainName2() throws Exception{
        LiteflowResponse response = flowExecutor.execute2Resp("chain2", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("chain2", context.getData("g"));
        Assert.assertEquals("sub1", context.getData("a"));
        Assert.assertEquals("sub2", context.getData("b"));
        Assert.assertEquals("sub3", context.getData("c"));
        Assert.assertEquals("sub4", context.getData("d"));
        Assert.assertEquals("sub5", context.getData("f"));
        Assert.assertEquals("sub5_chain2", context.getData("e"));
        Assert.assertEquals("sub6", context.getData("h"));
        Assert.assertEquals("sub6", context.getData("j"));
        Assert.assertNull(context.getData("k"));
    }

}
