package com.yomahub.liteflow.test.refreshRule;

import cn.hutool.core.io.resource.ResourceUtil;
import com.yomahub.liteflow.core.FlowExecutor;
import com.yomahub.liteflow.enums.FlowParserTypeEnum;
import com.yomahub.liteflow.flow.FlowBus;
import com.yomahub.liteflow.flow.LiteflowResponse;
import com.yomahub.liteflow.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.noear.solon.annotation.Inject;
import org.noear.solon.test.SolonJUnit4ClassRunner;
import org.noear.solon.test.annotation.TestPropertySource;

/**
 * springboot环境下重新加载规则测试
 * @author Bryan.Zhang
 * @since 2.6.4
 */
@RunWith(SolonJUnit4ClassRunner.class)
@TestPropertySource("classpath:/refreshRule/application.properties")
public class RefreshRuleELSpringbootTest extends BaseTest {

    @Inject
    private FlowExecutor flowExecutor;

    //测试普通刷新流程的场景
    @Test
    public void testRefresh1() throws Exception{
        String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.el.xml");
        FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, content);
        LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
        Assert.assertTrue(response.isSuccess());
    }

    //测试优雅刷新的场景
    @Test
    public void testRefresh2() throws Exception{
        new Thread(() -> {
            try {
                Thread.sleep(3000L);
                String content = ResourceUtil.readUtf8Str("classpath: /refreshRule/flow_update.el.xml");
                FlowBus.refreshFlowMetaData(FlowParserTypeEnum.TYPE_EL_XML, content);
            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

        for (int i = 0; i < 500; i++) {
            LiteflowResponse response = flowExecutor.execute2Resp("chain1", "arg");
            Assert.assertTrue(response.isSuccess());
            try {
                Thread.sleep(10L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}
