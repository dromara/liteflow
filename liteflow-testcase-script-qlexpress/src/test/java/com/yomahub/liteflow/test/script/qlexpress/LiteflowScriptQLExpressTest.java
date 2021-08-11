package com.yomahub.liteflow.test.script.qlexpress;

import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
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
 * 测试springboot下的脚本组件
 * @author Bryan.Zhang
 * @since 2.6.0
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/script/application.properties")
@SpringBootTest(classes = LiteflowScriptQLExpressTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.qlexpress.cmp"})
public class LiteflowScriptQLExpressTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试普通脚本节点
    @Test
    public void testScript1() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals(Integer.valueOf(6), response.getSlot().getData("s1"));
    }

    //测试条件脚本节点
    @Test
    public void testScript2() {
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(response.isSuccess());
        Assert.assertEquals("d==>s2==>b", response.getSlot().printStep());
    }
}
