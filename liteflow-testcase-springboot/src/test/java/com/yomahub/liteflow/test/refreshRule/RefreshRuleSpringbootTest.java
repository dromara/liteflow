package com.yomahub.liteflow.test.refreshRule;

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
 * springboot环境下重新加载规则测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SpringRunner.class)
@TestPropertySource(value = "classpath:/refreshRule/application.properties")
@SpringBootTest(classes = RefreshRuleSpringbootTest.class)
@EnableAutoConfiguration
@ComponentScan({"com.yomahub.liteflow.test.refreshRule.cmp"})
public class RefreshRuleSpringbootTest extends BaseTest {

    @Resource
    private FlowExecutor flowExecutor;

    //测试普通刷新流程的场景
    @Test
    public void testRefresh1() throws Exception{
        String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.xml");
        FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_XML, content);
        LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }

    //测试优雅刷新的场景
    @Test
    public void testRefresh2() throws Exception{
        new Thread(() -> {
            try {
                Thread.sleep(1000L);
                String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.xml");
                FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_XML, content);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

        for (int i = 0; i < 500; i++) {
            LiteflowResponse<DefaultSlot> response = flowExecutor.execute2Resp("chain1", "arg");
            Assert.assertTrue(response.isSuccess());
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
