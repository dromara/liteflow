package com.yomahub.liteflow.test.script.graaljs.meta;

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
 * 测试与Java交互的元数据
 * @author zendwang
 * @since 2.9.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/meta/application.properties")
@SpringBootTest(classes = LiteflowXmlScriptMetaELTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.graaljs.meta.cmp"})
public class LiteflowXmlScriptMetaELTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    @Test
    public void testMeta() {
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        DefaultContext context = response.getFirstContextBean();
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("chain1", context.getData("currChainId"));
        Assert.assertEquals("arg", context.getData("requestData"));
        Assert.assertEquals("s1", context.getData("nodeId"));
    }
}
