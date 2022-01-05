package com.yomahub.liteflow.test.script.qlexpress;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.entity.data.DefaultSlot;
import com.yomahub.liteflow.entity.data.LiteflowResponse;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
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
@TestPropertySource(value = "classpath:/xml-script/application.properties")
@SpringBootTest(classes = LiteflowXmlScriptQLExpressTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.script.qlexpress.cmp"})
public class LiteflowXmlScriptQLExpressTest extends BaseTest {

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
        Assert.assertEquals("d==>s2[条件脚本]==>b", response.getSlot().getExecuteStepStr());
    }

    @Test
    public void testScript3() throws Exception{
        //根据配置，加载的应该是flow.xml，执行原来的规则
        LiteflowResponse<DefaultSlot> responseOld = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(responseOld.isSuccess());
        Assert.assertEquals("d==>s2[条件脚本]==>b", responseOld.getSlot().getExecuteStepStr());
        //更改规则，重新加载，更改的规则内容从flow_update.xml里读取，这里只是为了模拟下获取新的内容。不一定是从文件中读取
        String newContent = ResourceUtil.readUtf8Str("classpath: /xml-script/flow_update.xml");
        //进行刷新
        FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_XML, newContent);

        //重新执行chain2这个链路，结果会变
        LiteflowResponse<DefaultSlot> responseNew = flowExecutor.execute2Resp("chain2", "arg");
        Assert.assertTrue(responseNew.isSuccess());
        Assert.assertEquals("d==>s2[条件脚本_改]==>a==>s3[普通脚本_新增]", responseNew.getSlot().getExecuteStepStr());
    }
}
